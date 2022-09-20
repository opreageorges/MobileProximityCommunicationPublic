package com.ogeorges.mobileproximitycommunication.localdatabase

import android.content.Context
import androidx.room.Room
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBMessage
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.models.Friend
import com.ogeorges.mobileproximitycommunication.models.User
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.*

object LocalDB: Observable() {
    private lateinit var connectionsDB: LocalDBImplementation
    private var friendList: MutableSet<DBFriend>? = mutableSetOf()
    var friendListObservedSize = 0
    val friendListCurrentSize = {friendList!!.size}
    private val messageMap = mutableMapOf<DBFriend, MutableList<DBMessage>>()
    var globalFriend: DBFriend? = null
    var openFriend: DBFriend? = null

    fun init(context: Context){
        connectionsDB = Room.databaseBuilder(
            context,
            LocalDBImplementation::class.java, "my_connections_and_chats"
        ).fallbackToDestructiveMigration().build()

        runBlocking {
            val proc = launch {
                friendList =  connectionsDB.friendDAO().getAll().toMutableSet()
                friendListObservedSize = friendList?.size!!
            }
            proc.invokeOnCompletion {
                launch {
                    for(friend in friendList!!){
                        if (friend.publicKey == Beacon.PAYLOAD_GLOBAL){
                            globalFriend = friend
                            friendList!!.remove(globalFriend)
                        }
                        val messages = connectionsDB.messageDAO().loadAllByUserPK(friend.publicKey)
                        messageMap[friend] = messages.toMutableList()
                    }
                    if(globalFriend == null){
                        val global = DBFriend(null, "", "", "", Beacon.PAYLOAD_GLOBAL, Friend.ACCEPTED, "")
                        connectionsDB.friendDAO().insertAll(global)
                        messageMap[global] = mutableListOf()
                        globalFriend = global
                    }
                }
            }
        }
    }

    override fun notifyObservers(arg: Any?) {
        setChanged()
        super.notifyObservers(arg)
    }

    fun addFriend(friend: DBFriend){
        friendList?.add(friend)
        notifyObservers(friend)
    }

    fun removeFriend(friend: DBFriend){
        friendList?.remove(friend)
        notifyObservers(friend)
    }

    fun modifyFriend(friend: DBFriend){
        val oldFriend = friendList!!.find { it.id == friend.id || it.lastEndpointID == friend.lastEndpointID}
        friendList?.remove(oldFriend)
        friendList?.add(friend)
        notifyObservers(friend)
    }

    fun modifyFriend(user: User){
        val oldFriend = friendList!!.find { Friend.fromDBFriend(it).id == user.id} ?: return
        friendList?.remove(oldFriend)
        val friend = Friend(user, oldFriend.status!!)
        friendList?.add(friend.toDBFriend())
        notifyObservers(friend.toDBFriend())
    }

    fun updateLastEndpoint(id: Int, newEndpointId: String){
        val friend = friendList!!.find { Friend.fromDBFriend(it).id == id} ?: return
        friend.lastEndpointID = newEndpointId
        notifyObservers(friend)
    }

    fun readFriendList(): List<DBFriend>?{
        return friendList?.toList()
    }

    fun connectionDrop(endpointId: String){
        val friend = friendList!!.find { it.lastEndpointID == endpointId} ?: return
        if (friend.status != Friend.ACCEPTED){
            friend.status = Friend.DECLINED
        }
        notifyObservers(friend)
    }

    fun addMessageWith(message: DBMessage, friend:DBFriend?){
        val notNullFriend = friend ?: globalFriend

        if(messageMap[notNullFriend] != null){
            messageMap[notNullFriend]?.add(message)
        }
        else{
            messageMap[notNullFriend!!] = mutableListOf(message)
        }
        notifyObservers(message)
    }

    fun clearMessagesWith(friend: DBFriend?){
        val notNullFriend = friend ?: globalFriend
        messageMap[notNullFriend]?.clear()
        if (messageMap[notNullFriend] != null) {
            runBlocking {
                launch {
                    for (message in messageMap[notNullFriend]!!) {
                        connectionsDB.messageDAO().delete(message)
                    }
                }
            }
        }
        notifyObservers("clear")
    }

    fun readMessageListOf(friend:DBFriend?): List<DBMessage>?{
        val notNullFriend = friend ?: globalFriend
        return messageMap[notNullFriend]?.toList()
    }

    fun save(){
        runBlocking {
            launch {
                if (friendList == null) return@launch

                val oldFriends = connectionsDB.friendDAO().getAll()
                for (of in oldFriends){
                    val f = friendList!!.find { of.publicKey == it.publicKey }
                    if (f == null) connectionsDB.friendDAO().delete(of)
                    else connectionsDB.friendDAO().update(of)
                }
                for (friend in friendList!!) {
                    val f = oldFriends.find { friend.publicKey == it.publicKey }
                    if(f != null) return@launch
                    if (friend.status == Friend.ACCEPTED) {
                        connectionsDB.friendDAO().insertAll(friend)
                    }
                }
                connectionsDB.friendDAO().insertAll(globalFriend!!)
            }.invokeOnCompletion {
                launch {
                    for (key in messageMap.keys){
                        if (messageMap[key] == null) continue
                        connectionsDB.messageDAO().insertAll(*(messageMap[key]!!.toTypedArray()))
                    }
                }
            }
        }
    }

    fun clearFriends(){
        runBlocking {
            launch {
                if (friendList == null) return@launch

                val oldFriends = connectionsDB.friendDAO().getAll()

                connectionsDB.friendDAO().delete(*oldFriends.toTypedArray())
            }
        }
        val oldFriends = friendList!!.toList()
        friendList!!.clear()
        notifyObservers(oldFriends)
    }
}