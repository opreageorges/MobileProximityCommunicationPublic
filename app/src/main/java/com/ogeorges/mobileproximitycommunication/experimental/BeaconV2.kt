package com.ogeorges.mobileproximitycommunication.experimental

import android.app.Activity
import android.content.Context
import com.google.android.gms.nearby.Nearby
import com.google.android.gms.nearby.connection.*
import com.ogeorges.mobileproximitycommunication.adapters.UserToChatButtonAdapter
import com.ogeorges.mobileproximitycommunication.adapters.UserToNewChatButtonAdapter
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.models.Friend
import com.ogeorges.mobileproximitycommunication.models.User
import java.io.File
import java.io.FileInputStream

object BeaconV2 {
    private lateinit var connectionsClient: ConnectionsClient
    var isAvailable:Boolean = false
    var isLooking: Boolean = false
    private val strategy = Strategy.P2P_CLUSTER
    private lateinit var activity: Activity

    fun init(activity: Activity){
        connectionsClient = Nearby.getConnectionsClient(activity)
        this.activity = activity
    }

    fun startDiscovery(adapter: UserToNewChatButtonAdapter){
        Beacon.stop()
        isLooking = true
        val options = DiscoveryOptions.Builder().setStrategy(strategy).build()

        val endpointDiscoveryCallback = object : EndpointDiscoveryCallback() {
            override fun onEndpointFound(endpointId: String, info: DiscoveredEndpointInfo) {
                val u = User.fromJson(info.endpointName)
                u.stringid = endpointId
                adapter.add(u)
            }

            override fun onEndpointLost(endpointId: String) {
            }
        }

        connectionsClient.startDiscovery(activity.packageName,endpointDiscoveryCallback,options)
    }

    fun stopDiscovery(){
        isLooking = false
        connectionsClient.stopDiscovery()
        if (!isLooking && !isAvailable){
            Beacon.start()
        }
    }

    fun connect(endpointId: String, f:Friend, adapter: UserToChatButtonAdapter){
        val payloadCallback = object : PayloadCallback(){
            var iddata: Long = 0
            var idimg: Long = 0

            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                if (payload.type == Payload.Type.BYTES){
                    iddata = payload.id
                    val completeFriend = Friend(
                        User.fromJson(String(payload.asBytes()!!)),
                        Friend.ACCEPTED
                    )
                    completeFriend.avatarimg = ""
                    f.update(completeFriend)
                    adapter.notifyDataSetChanged()
                    LocalDB.addFriend(f.toDBFriend())
                }
                else if (payload.type == Payload.Type.STREAM){
                    idimg = payload.id
                    activity.runOnUiThread {
                        val inputStream = payload.asStream()?.asInputStream()
                        val outputStream = activity.openFileOutput("AvatarOf${f.id}", Context.MODE_PRIVATE)
                        outputStream.write(inputStream?.readBytes())
                        f.avatarimg = activity.filesDir.path + "/AvatarOf${f.id}"
                        adapter.notifyDataSetChanged()
                        connectionsClient.disconnectFromEndpoint(endpointId)
                    }
                }
            }

            override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
                p1.payloadId
                p1.status
            }

        }

        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                connectionsClient.acceptConnection(endpointId, payloadCallback)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                println(endpointId)
                if (result.status.isSuccess) {
                    f.status = Friend.ACCEPTED
                    adapter.notifyDataSetChanged()
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                    val payload = Payload.fromBytes(User.toJson(User.getSelf(activity)).toByteArray(Charsets.UTF_8))
                    connectionsClient.sendPayload(endpointId,payload)
                    val imgPath = User.getSelf(activity).avatarimg
                    if(imgPath != "" && imgPath != null){
                        val myProfileImage = File(User.getSelf(activity).avatarimg!!)
                        val fis = FileInputStream(myProfileImage)
                        val profilePicture = Payload.fromStream(fis)
                        connectionsClient.sendPayload(endpointId, profilePicture)
                    }
                }
                else{
                    f.status = Friend.DECLINED
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onDisconnected(endpointId: String) {
                println(endpointId)
            }
        }
        val self = User.getSelf(activity)
        self.avatarimg = ""
        self.publicKeyString = ""
        connectionsClient.requestConnection(User.toJson(self), endpointId, connectionLifecycleCallback)
    }

    fun accept(endpointId: String, f: Friend, adapter: UserToChatButtonAdapter){
        val payloadCallback: PayloadCallback = object : PayloadCallback() {
            override fun onPayloadReceived(endpointId: String, payload: Payload) {
                if (payload.type == Payload.Type.BYTES){
                    val completeFriend = Friend(
                        User.fromJson(String(payload.asBytes()!!)),
                        Friend.ACCEPTED
                    )
                    completeFriend.avatarimg = ""
                    f.update(completeFriend)
                }
                else if (payload.type == Payload.Type.STREAM){
                    activity.runOnUiThread {
                        val inputStream = payload.asStream()?.asInputStream()
                        val outputStream = activity.openFileOutput("AvatarOf${f.id}", Context.MODE_PRIVATE)
                        outputStream.write(inputStream?.readBytes())
                        inputStream?.close()
                        outputStream.close()
                        f.avatarimg = activity.filesDir.path + "/AvatarOf${f.id}"
                        adapter.notifyDataSetChanged()
                        connectionsClient.disconnectFromEndpoint(endpointId)
                        stop()
                    }
                }
            }

            override fun onPayloadTransferUpdate(p0: String, p1: PayloadTransferUpdate) {
            }
        }

        connectionsClient.acceptConnection(endpointId, payloadCallback)
        val payload = Payload.fromBytes(User.toJson(User.getSelf(activity)).toByteArray(Charsets.UTF_8))
        connectionsClient.sendPayload(endpointId,payload)
        val imgPath = User.getSelf(activity).avatarimg
        if(imgPath != null && imgPath != ""){
            val myProfileImage = File(User.getSelf(activity).avatarimg!!)
            val inputStream = FileInputStream(myProfileImage)
            val profilePicture = Payload.fromStream(inputStream)
            connectionsClient.sendPayload(endpointId, profilePicture)
        }

    }

    fun reject(endpointId: String){
        connectionsClient.rejectConnection(endpointId)
    }

    fun startAdvertising(adapter: UserToChatButtonAdapter) {
        Beacon.stop()

        isAvailable = true

        val connectionLifecycleCallback = object : ConnectionLifecycleCallback() {
            override fun onConnectionInitiated(endpointId: String, info: ConnectionInfo) {
                val u = User.fromJson(info.endpointName)
                val f = Friend(u, Friend.REQUEST)
                f.stringid = endpointId
                adapter.add(f)
            }

            override fun onConnectionResult(endpointId: String, result: ConnectionResolution) {
                if (result.status.isSuccess) {
                    connectionsClient.stopAdvertising()
                    connectionsClient.stopDiscovery()
                }
            }

            override fun onDisconnected(endpointId: String) {
            }
        }

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

    fun stopAdvertising(){
        isAvailable = false
        connectionsClient.stopAdvertising()
        if (!isLooking && !isAvailable){
            Beacon.start()
        }
    }

    fun stop(){
        isLooking = false
        isAvailable = false
        connectionsClient.apply {
            stopAdvertising()
            stopDiscovery()
            stopAllEndpoints()
        }
    }
}