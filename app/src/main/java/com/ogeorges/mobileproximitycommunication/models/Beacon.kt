package com.ogeorges.mobileproximitycommunication.models

import android.app.Activity
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBMessage
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

object Beacon: Observable() {
    private lateinit var connectionsClient: ConnectionsClient
    private val strategy = Strategy.P2P_CLUSTER
    private lateinit var activity: Activity

    private var isOn = false

    var observedReachablePeopleSize = 0
    val currentReachablePeopleSize = { reachablePeople.size}

    private val reachablePeople = ArrayList<User>()
    private val peopleNearby = ArrayList<User>()

    // Padding of 0 until it reaches 10 chars
    const val PAYLOAD_HANDSHAKE_REQUEST = "Request000"
    const val PAYLOAD_HANDSHAKE_ACCEPT = "Accept0000"
    const val PAYLOAD_HANDSHAKE_DECLINE = "Decline000"
    private const val PAYLOAD_PM = "PMMessage0"
    const val PAYLOAD_GLOBAL = "Global0000"
    private const val MAX_ID = Int.MIN_VALUE.toString().length

    fun init(activity: Activity){
        connectionsClient = Nearby.getConnectionsClient(activity)
        Beacon.activity = activity

    }

    fun start(){
        if (isOn) return
        startAdvertising()
        startDiscovery()
        isOn = true
    }

    override fun notifyObservers(arg: Any?) {
        setChanged()
        super.notifyObservers(arg)
    }

    fun readReachablePeople(): List<User> {
        return reachablePeople.toList()
    }

    fun sendHandShakeMessage(endpointID: String, type: String){
        val payload:Payload = when(type){
            PAYLOAD_HANDSHAKE_REQUEST ->{
                val self = User.getSelf(activity)
                self.avatarimg = ""

                Payload.fromBytes("$PAYLOAD_HANDSHAKE_REQUEST${User.toJson(self)}".toByteArray())
            }
            PAYLOAD_HANDSHAKE_ACCEPT ->{
                val self = User.getSelf(activity)
                self.avatarimg = ""
                Payload.fromBytes("$PAYLOAD_HANDSHAKE_ACCEPT${User.toJson(self)}".toByteArray())
            }
            PAYLOAD_HANDSHAKE_DECLINE ->{
                val self = User.getSelf(activity)
                self.avatarimg = ""
                self.realname = ""
                self.publicKeyString = ""
                Payload.fromBytes("$PAYLOAD_HANDSHAKE_DECLINE${User.toJson(self)}".toByteArray())
            }
            else -> {
                Payload.fromBytes("".toByteArray())
            }
        }
        connectionsClient.sendPayload(endpointID, payload)
    }

    fun sendPrivateMessage(content: String, friend: Friend?){
        val id = User.getSelf(activity).id
        var paddedId = id.toString()

        while (paddedId.length < MAX_ID ){
            paddedId = "0$paddedId"
        }
        if (friend != null) {
            val encryptedContent = Cryptor.encryptMessage(content, friend.getPublicKey())
            val payload = Payload.fromBytes("$PAYLOAD_PM$paddedId$encryptedContent".toByteArray())
            connectionsClient.sendPayload(friend.stringid, payload)
        }
        else{
            val payload = Payload.fromBytes("$PAYLOAD_GLOBAL$paddedId$content".toByteArray())
            for (user in reachablePeople) connectionsClient.sendPayload(user.stringid, payload)
        }
    }

    private val payloadCallback = object : PayloadCallback(){
        override fun onPayloadReceived(endpointId: String, payload: Payload) {
            if (payload.type == Payload.Type.BYTES){
                val payloadContent = String(payload.asBytes()!!)
                val type = payloadContent.take(10)
                var content = payloadContent.takeLast(payloadContent.length - 10)
                when (type){
                    PAYLOAD_HANDSHAKE_REQUEST ->{
                        val f = Friend(User.fromJson(content), Friend.REQUEST)
                        val avatarImg = File(activity.filesDir.path +  "/TEMPAvatarOf$endpointId")
                        if(avatarImg.exists()){
                            runBlocking {
                                val inputStream = FileInputStream(avatarImg)
                                f.avatarimg = activity.filesDir.path +  "/AvatarOf${f.id}.jpeg"
                                f.stringid = endpointId
                                val outputStream = FileOutputStream(File(f.avatarimg!!))
                                outputStream.write(inputStream.readBytes())
                                outputStream.close()
                                inputStream.close()
                                LocalDB.modifyFriend(f.toDBFriend())
                            }
                        }
                        f.stringid = endpointId
                        LocalDB.addFriend(f.toDBFriend())
                    }
                    PAYLOAD_HANDSHAKE_ACCEPT ->{
                        val f = Friend(User.fromJson(content), Friend.ACCEPTED)
                        f.stringid = endpointId
                        LocalDB.modifyFriend(f.toDBFriend())
                    }
                    PAYLOAD_HANDSHAKE_DECLINE ->{
                        val f = Friend(User.fromJson(content), Friend.DECLINED)
                        f.stringid = endpointId
                        LocalDB.modifyFriend(f.toDBFriend())
                    }

                    PAYLOAD_PM ->{
                        val paddedID = content.take(MAX_ID).split("-")

                        val senderID: Int = if(paddedID.size == 2){
                            "-${paddedID[1]}".toInt()
                        } else{
                            paddedID[0].toInt()
                        }

                        content = content.takeLast(content.length - MAX_ID)
                        val dbFriend = LocalDB.readFriendList()?.find { Friend.fromDBFriend(it, activity).id == senderID } ?: return
                        content = Cryptor.decryptMessage(content)
                        val message = DBMessage(0,content,dbFriend.publicKey,ChatMessage.SENDER_TYPE_YOU, false, Calendar.getInstance().time.time)
                        LocalDB.addMessageWith(message, dbFriend)
                    }
                    PAYLOAD_GLOBAL -> {
                        content = content.takeLast(content.length - MAX_ID)
                        val message = DBMessage(0,content, PAYLOAD_GLOBAL,ChatMessage.SENDER_TYPE_YOU, true, Calendar.getInstance().time.time)
                        LocalDB.addMessageWith(message, null)
                    }
                }
            }
            else if (payload.type == Payload.Type.STREAM){
                val stream = payload.asStream()!!

                runBlocking {
                    val filename = "TEMPAvatarOf$endpointId"
                    val outputFile = File(activity.filesDir.path + "/$filename")
                    val outputStream = FileOutputStream(outputFile)
                    outputStream.write(stream.asInputStream().readBytes())
                    stream.asInputStream().close()
                    outputStream.close()

                }
            }
        }

        override fun onPayloadTransferUpdate(endpointId: String, ptu: PayloadTransferUpdate) {
            return
        }

    }

    private val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
        override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
            connectionsClient.acceptConnection(endpointId, payloadCallback)

            val u = User.fromJson(info.endpointName)
            u.stringid = endpointId
            if (u in reachablePeople){
                return
            }

            LocalDB.updateLastEndpoint(u.id, u.stringid)
            reachablePeople.add(u)
            notifyObservers(u)
        }

        override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
            if(result.status.isSuccess){
                val self = User.getSelf(activity)
                if (self.avatarimg != "" && self.avatarimg != null) {
                    val stream = FileInputStream(self.avatarimg)
                    val streamPayload = Payload.fromStream(stream)
                    connectionsClient.sendPayload(endpointId, streamPayload)
                }
            }
        }

        override fun onDisconnected(endpointId: String) {
            val u = reachablePeople.find { it.stringid == endpointId } ?: return
            reachablePeople.remove(u)
            LocalDB.connectionDrop(endpointId)
            notifyObservers(u)
        }
    }

    private fun startDiscovery(){
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()

        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                val u = User.fromJson(info.endpointName)
                if (u in peopleNearby) return

                u.stringid = endpointId
                peopleNearby.add(u)
                val self = User.getSelf(activity)
                self.realname = ""
                self.avatarimg = ""
                self.publicKeyString = ""
                connectionsClient.requestConnection(
                    User.toJson(self),
                    endpointId,
                    connectionLifecycleCallback
                )
            }

            override fun onEndpointLost(endpointId: String) {
                peopleNearby.removeIf{it.stringid == endpointId}
            }
        }

        connectionsClient.startDiscovery(activity.packageName,endpointDiscoveryCallback,options)
    }

    private fun startAdvertising() {
        val options = AdvertisingOptions.Builder().setStrategy(strategy).build()
        val self = User.getSelf(activity)
        self.realname = ""
        self.avatarimg = ""
        self.publicKeyString = ""
        connectionsClient.startAdvertising(
            User.toJson(self),
            activity.packageName,
            connectionLifecycleCallback,
            options
        )
    }

    fun stop(){
        isOn = false
        reachablePeople.clear()
        peopleNearby.clear()
        observedReachablePeopleSize = 0
        connectionsClient.apply {
            stopAdvertising()
            stopDiscovery()
            stopAllEndpoints()
        }
    }
}