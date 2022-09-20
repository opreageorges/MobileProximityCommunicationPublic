package com.ogeorges.mobileproximitycommunication.vmodels

import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.findFragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.adapters.MessageToChatBubbleAdapter
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.experimental.TestFragment2
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBMessage
import com.ogeorges.mobileproximitycommunication.models.ChatMessage
import com.ogeorges.mobileproximitycommunication.models.Friend
import com.ogeorges.mobileproximitycommunication.models.User
import com.ogeorges.mobileproximitycommunication.views.ChatFragment
import com.ogeorges.mobileproximitycommunication.views.FunctionalitySettingsFragment
import com.ogeorges.mobileproximitycommunication.views.ProfileFragment
import java.util.*

class ChatViewModel(val friend: DBFriend?) : ViewModel() {

    private lateinit var localDBMessageObserver: LocalDBMessageObserver

    inner class LocalDBMessageObserver(private val adapter: MessageToChatBubbleAdapter) : Observer {
        override fun update(p0: Observable?, p1: Any?) {
            if (p1 is String && p1 == "clear") adapter.clear()
            if (p1 == null || p1 !is DBMessage) return
            if (p1.senderId == friend?.publicKey || (p1.senderId == Beacon.PAYLOAD_GLOBAL && friend == null)){
                p1.isReceived = true
                adapter.add(ChatMessage(p1))
            }
        }
    }

    fun initMessagesArea(chatMessagesArea: RecyclerView) {
        val manager = LinearLayoutManager(chatMessagesArea.context)
        manager.orientation = LinearLayoutManager.VERTICAL
        manager.stackFromEnd = true
        manager.isSmoothScrollbarEnabled = true

        chatMessagesArea.layoutManager = manager

        LocalDB.openFriend = friend ?: LocalDB.globalFriend

        val adapter = MessageToChatBubbleAdapter(mutableSetOf())
        chatMessagesArea.adapter = adapter

        adapter.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                manager.smoothScrollToPosition(chatMessagesArea, null, adapter.itemCount)
            }
        })

        localDBMessageObserver = LocalDBMessageObserver(adapter)
        LocalDB.addObserver(localDBMessageObserver)


        val messages = LocalDB.readMessageListOf(friend)
        if (messages != null) {
            for (message in messages) {
                adapter.add(ChatMessage(message))
                message.isReceived = true
            }
        }


    }

    fun sendMessage(view: View){
        val fragment = view.findFragment<ChatFragment>()

        val textInput = fragment.binding.textInputLayout

        if (textInput.text?.trim() == "") return

        val message = ChatMessage(
            textInput.text?.trim().toString(),
            User.getSelf(view.context).username,
            ChatMessage.SENDER_TYPE_YOU,
            Date().time
        )

        textInput.text?.clear()
        val dbMessage : DBMessage
        if(friend != null) {
            dbMessage = DBMessage(
                null,
                message.body.toString(),
                friend.publicKey,
                ChatMessage.SENDER_TYPE_ME,
                true,
                Calendar.getInstance().time.time
            )
            Beacon.sendPrivateMessage(message.body.toString(), Friend.fromDBFriend(friend))
        }
        else {
            dbMessage = DBMessage(
                null,
                message.body.toString(),
                Beacon.PAYLOAD_GLOBAL,
                ChatMessage.SENDER_TYPE_ME,
                true,
                Calendar.getInstance().time.time
            )
            Beacon.sendPrivateMessage(message.body.toString(), null)
        }
        LocalDB.addMessageWith(dbMessage, friend)

    }

    fun stop(){
        LocalDB.deleteObserver(localDBMessageObserver)
        LocalDB.openFriend = null
    }

    fun initToolBar(toolbar: androidx.appcompat.widget.Toolbar) {
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.title = friend?.realName
        toolbar.menu.findItem(R.id.main_menu_public_chat_button).isVisible = false
        val mainMenuClear = toolbar.menu.findItem(R.id.main_menu_clear)
        mainMenuClear.isVisible = true
        mainMenuClear.setTitle(R.string.clear_messages)

        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.main_menu_clear->{
                    LocalDB.clearMessagesWith(friend)
                    true
                }
                R.id.main_menu_experimental ->{
                    val fragmentManager = toolbar.findFragment<Fragment>().parentFragmentManager
                    fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, TestFragment2()).commit()
                    true
                }
                R.id.main_menu_settings_button ->{
                    val fragmentManager = toolbar.findFragment<Fragment>().parentFragmentManager
                    fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, FunctionalitySettingsFragment()).commit()
                    true
                }

                R.id.main_menu_edit_profile_button -> {
                    val fragmentManager = toolbar.findFragment<Fragment>().parentFragmentManager
                    fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ProfileFragment()).addToBackStack(null).commit()

                    true
                }
                else -> {
                    false
                }
            }
        }
    }



}