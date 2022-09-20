package com.ogeorges.mobileproximitycommunication.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.adapters.UserToChatButtonAdapter
import com.ogeorges.mobileproximitycommunication.databinding.FragmentChatListBinding
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBMessage
import com.ogeorges.mobileproximitycommunication.models.Friend
import com.ogeorges.mobileproximitycommunication.vmodels.ChatListViewModel
import java.util.*


class ChatListFragment : Fragment() {

    lateinit var binding: FragmentChatListBinding
    private lateinit var viewModel: ChatListViewModel
    private lateinit var localDBObserver: LocalDBObserver

    inner class LocalDBObserver(private val adapter: UserToChatButtonAdapter): Observer {

        override fun update(p0: Observable?, p1: Any?) {
            if (p1 == null) return

            if (p1 is DBMessage){
                val sentByFriend = LocalDB.readFriendList()?.find { it.publicKey == p1.senderId } ?: return
                adapter.increaseUnreadMessageCount(sentByFriend)
            }

            if(p1 is DBFriend) {
                val currentSize = LocalDB.friendListCurrentSize.invoke()
                val observedSize = LocalDB.friendListObservedSize
                val friend = Friend.fromDBFriend(p1, activity!!)

                if (currentSize < observedSize) {
                    adapter.remove(friend)
                } else if (currentSize > observedSize) {
                    adapter.add(friend)
                } else {
                    adapter.modify(friend)
                }
                adapter.notifyDataSetChanged()
                LocalDB.friendListObservedSize = currentSize

            }
            else if(p1 is List<*>){
                for (dbFriend in p1){
                    if(dbFriend == null) return
                    val friend = Friend.fromDBFriend(dbFriend as DBFriend, activity!!)
                    adapter.remove(friend)
                }
                LocalDB.friendListObservedSize = 0
            }

        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        R.navigation.main_navigation
        binding = FragmentChatListBinding.inflate(inflater, container, false)
        viewModel = ChatListViewModel()
        binding.viewModel = viewModel

        viewModel.initToolbar(binding.toolbar, this)

        val rw = binding.activeChatsList
        val manager = LinearLayoutManager(rw.context)
        manager.orientation = LinearLayoutManager.VERTICAL
        rw.layoutManager = manager

        val adapter = UserToChatButtonAdapter(ArrayList(), mutableMapOf()){ friend ->
            val dbFriend = LocalDB.readFriendList()!!.find {
                it.publicKey == friend.publicKeyString } ?: return@UserToChatButtonAdapter

            val transaction =activity?.supportFragmentManager?.beginTransaction()
                    ?.replace(R.id.fragmentContainerView, ChatFragment(dbFriend))

            transaction?.addToBackStack(null)
            transaction?.commit()

        }

        rw.adapter = adapter

        for (friend in LocalDB.readFriendList()!!){
            val f = Friend.fromDBFriend(friend, activity!!)
            adapter.add(f)
            val unreadMessageCount = LocalDB.readMessageListOf(friend)?.filter { !it.isReceived }?.size ?: 0
            adapter.setUnreadMessageCount(f,unreadMessageCount)
            rw.scrollToPosition(adapter.itemCount - 1)
        }

        localDBObserver = LocalDBObserver(adapter)
        LocalDB.addObserver(localDBObserver)

        Beacon.init(activity!!)
        Beacon.start()

        return binding.root
    }

    override fun onResume() {
        Beacon.start()
        super.onResume()
    }

    override fun onDestroyView() {
        LocalDB.deleteObserver(localDBObserver)
        LocalDB.save()
        super.onDestroyView()
    }

}