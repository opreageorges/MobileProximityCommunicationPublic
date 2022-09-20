package com.ogeorges.mobileproximitycommunication.adapters

import android.graphics.Color
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import com.ogeorges.mobileproximitycommunication.models.ChatMessage
import com.ogeorges.mobileproximitycommunication.models.Friend

class UserToChatButtonAdapter(private val dataSet: ArrayList<Friend>, private val unreadMessagesMap: MutableMap<Friend, Int>, private val clickListener:(Friend)-> Unit) : RecyclerView.Adapter<UserToChatButtonAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val nameTextView: TextView
        val lastMessageTextView: TextView
        val avatarImageView: ImageView
        val acceptButton: Button
        val declineButton: Button
        val available: ImageView
        val unreadMessages: TextView

        init {
            nameTextView = view.findViewById(R.id.chat_button_name)
            lastMessageTextView = view.findViewById(R.id.chat_button_last_message)
            avatarImageView = view.findViewById(R.id.chat_button_avatar)
            acceptButton = view.findViewById(R.id.chat_button_accept)
            declineButton = view.findViewById(R.id.chat_button_decline)
            available = view.findViewById(R.id.chat_button_available)
            unreadMessages= view.findViewById(R.id.chat_button_unread_messages)
        }
    }

    fun add(f: Friend){
        dataSet.add(f)
        val index = dataSet.indexOf(f)
        notifyItemInserted(index)
    }

    fun remove(f: Friend){
        val index = dataSet.indexOf(f)
        dataSet.remove(f)
        notifyItemRemoved(index)
    }

    fun modify(f:Friend){
        val index = dataSet.indexOf(f)
        val dataSetFriend = dataSet.find { f.stringid == it.stringid } ?: return
        remove(dataSetFriend)
        add(f)
        notifyItemChanged(index)

    }

    fun setUnreadMessageCount(friend: Friend, count: Int){
        unreadMessagesMap[friend] = count
        val index = dataSet.indexOf(friend)
        notifyItemChanged(index)
    }

    fun increaseUnreadMessageCount(dbFriend: DBFriend){
        val friend = dataSet.find { it.publicKeyString == dbFriend.publicKey } ?: return
        unreadMessagesMap[friend] = if (unreadMessagesMap[friend] == null) 1
        else unreadMessagesMap[friend]?.plus(1) as Int

        val index = dataSet.indexOf(friend)
        notifyItemChanged(index)
    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.chat_button, viewGroup, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        val friend = this.dataSet[position]
        val userDisplayName =  if (friend.realname != "") friend.realname else friend.username

        viewHolder.nameTextView.text = userDisplayName

        val lastMessage = friend.getLastMessage()

        if (unreadMessagesMap[friend] != null && unreadMessagesMap[friend] != 0){
            viewHolder.unreadMessages.visibility = VISIBLE
            viewHolder.unreadMessages.text = unreadMessagesMap[friend].toString()
        }
        else{
            viewHolder.unreadMessages.visibility = GONE
        }

        if (lastMessage == null){
            when(friend.status ) {
                Friend.ACCEPTED -> viewHolder.lastMessageTextView.setText(R.string.accepted)
                Friend.DECLINED -> viewHolder.lastMessageTextView.setText(R.string.declined)
                Friend.PENDING -> viewHolder.lastMessageTextView.setText(R.string.pending)
            }
        }
        else{
            viewHolder.lastMessageTextView.text = lastMessage
        }

        val imgPath = if (friend.avatarimg != null ) friend.avatarimg else ""

        viewHolder.avatarImageView.setImageURI(Uri.parse(imgPath))
        if (friend.status == Friend.REQUEST){
            viewHolder.lastMessageTextView.visibility = GONE
            viewHolder.available.visibility = GONE
            viewHolder.acceptButton.visibility = VISIBLE
            viewHolder.acceptButton.setOnClickListener {
                Beacon.sendHandShakeMessage(friend.stringid, Beacon.PAYLOAD_HANDSHAKE_ACCEPT)
                friend.status = Friend.ACCEPTED
                LocalDB.modifyFriend(friend.toDBFriend())
                notifyDataSetChanged()
            }
            viewHolder.declineButton.visibility = VISIBLE
            viewHolder.declineButton.setOnClickListener {
                Beacon.sendHandShakeMessage(friend.stringid, Beacon.PAYLOAD_HANDSHAKE_DECLINE)
                LocalDB.removeFriend(friend.toDBFriend())
                friend.status = Friend.DECLINED
                notifyDataSetChanged()
            }
        }
        else{
            viewHolder.acceptButton.visibility = GONE
            viewHolder.declineButton.visibility = GONE
            viewHolder.lastMessageTextView.visibility = VISIBLE
            viewHolder.available.visibility = VISIBLE
        }

        if(Beacon.readReachablePeople().find { it.id == friend.id } != null){
            viewHolder.available.clearColorFilter()
            viewHolder.available.setColorFilter(Color.GREEN)
        }
        else{
            viewHolder.available.clearColorFilter()
            viewHolder.available.setColorFilter(Color.RED)
        }

        if (friend.status == Friend.ACCEPTED){
            viewHolder.itemView.setOnClickListener {
                clickListener(friend)
            }
        }
    }

    override fun getItemCount() = dataSet.size


}