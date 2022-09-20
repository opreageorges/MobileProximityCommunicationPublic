package com.ogeorges.mobileproximitycommunication.experimental

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.adapters.UserToChatButtonAdapter
import com.ogeorges.mobileproximitycommunication.adapters.UserToNewChatButtonAdapter
import com.ogeorges.mobileproximitycommunication.databinding.FragmentChatListBinding
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.models.Friend
import com.ogeorges.mobileproximitycommunication.models.User
import com.ogeorges.mobileproximitycommunication.views.ChatFragment
import com.ogeorges.mobileproximitycommunication.views.FunctionalitySettingsFragment
import com.ogeorges.mobileproximitycommunication.views.ProfileFragment
import java.util.*

class TestFragment2: Fragment() {

    lateinit var binding: FragmentChatListBinding
    private lateinit var friendListObserver: FriendListObserver
    private lateinit var beaconObserver: BeaconObserver

    inner class FriendListObserver(private val adapter: UserToChatButtonAdapter): Observer{

        override fun update(p0: Observable?, p1: Any?) {
            if (p1 == null) return

            val currentSize = LocalDB.friendListCurrentSize.invoke()
            val observedSize = LocalDB.friendListObservedSize

            if(p1 is DBFriend) {
                val friend = Friend.fromDBFriend(p1, activity!!)

                if (currentSize < observedSize) {
                    adapter.remove(friend)
                } else if (currentSize > observedSize) {
                    adapter.add(friend)
                } else {
                    adapter.modify(friend)
                    adapter.notifyDataSetChanged()
                }

            }
            else if(p1 is List<*>){
                for (dbFriend in p1){
                    if(dbFriend == null) return
                    val friend = Friend.fromDBFriend(dbFriend as DBFriend, activity!!)

                    if (currentSize < observedSize) {
                        adapter.remove(friend)
                    } else if (currentSize > observedSize) {
                        adapter.add(friend)
                    } else {
                        adapter.modify(friend)
                    }
                }
            }
            LocalDB.friendListObservedSize = currentSize
        }

    }

    class BeaconObserver(private val adapter: UserToNewChatButtonAdapter): Observer{
        override fun update(p0: Observable?, user: Any?) {
            if (user !is User) return
            val currentSize = Beacon.currentReachablePeopleSize.invoke()
            val observedSize = Beacon.observedReachablePeopleSize

            if (currentSize < observedSize){
                adapter.remove(user)
            }
            else if (currentSize > observedSize){
                adapter.add(user)
            }

            adapter.notifyDataSetChanged()
            Beacon.observedReachablePeopleSize = currentSize
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatListBinding.inflate(inflater, container, false)
        initToolbar(binding.toolbar)
        val rw = binding.activeChatsList
        val manager = LinearLayoutManager(rw.context)
        manager.orientation = LinearLayoutManager.VERTICAL
        rw.layoutManager = manager

        val adapter = UserToChatButtonAdapter(ArrayList(), mutableMapOf()){ friend ->
            val dbFriend = LocalDB.readFriendList()!!.find { it.publicKey == friend.publicKeyString } ?: return@UserToChatButtonAdapter
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.fragmentContainerView, ChatFragment(dbFriend))
                ?.commit()

        }

        rw.adapter = adapter
        for (friend in LocalDB.readFriendList()!!){
            val f = Friend.fromDBFriend(friend, activity!!)
            adapter.add(f)
            rw.scrollToPosition(adapter.itemCount - 1)
        }

        friendListObserver = FriendListObserver(adapter)
        LocalDB.addObserver(friendListObserver)

        Beacon.init(activity!!)
        Beacon.start()
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.newChatActionButton.setOnLongClickListener {
            newChatActionButtonAction()
            true
        }
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroyView() {
        LocalDB.deleteObserver(friendListObserver)
        LocalDB.save()
        super.onDestroyView()
    }



    private fun newChatActionButtonAction(){
        val newChatDialog = Dialog(activity!!)

        newChatDialog.setCancelable(true)

        newChatDialog.setContentView(R.layout.new_chat_with)

        val newChatWithPeopleList: RecyclerView = newChatDialog.findViewById(R.id.new_chat_with_people_list)

        val manager = LinearLayoutManager(newChatWithPeopleList.context)
        manager.orientation = LinearLayoutManager.VERTICAL
        newChatWithPeopleList.layoutManager = manager

        val adapter = UserToNewChatButtonAdapter(ArrayList()) {
            val f = Friend(it, Friend.PENDING)
            f.update(it)
            Beacon.sendHandShakeMessage(it.stringid, Beacon.PAYLOAD_HANDSHAKE_REQUEST)
            LocalDB.addFriend(f.toDBFriend())
            newChatDialog.dismiss()
        }
        newChatWithPeopleList.adapter = adapter
        for (elem in Beacon.readReachablePeople()){
            adapter.add(elem)
        }
        beaconObserver = BeaconObserver(adapter)
        Beacon.addObserver(beaconObserver)

        newChatDialog.setOnDismissListener {
            Beacon.deleteObserver(beaconObserver)
        }

        val newChatWithSearchBar: SearchView = newChatDialog.findViewById(R.id.new_chat_with_search_bar)

        newChatWithSearchBar.setOnQueryTextListener(object : SearchView.OnQueryTextListener{
            override fun onQueryTextSubmit(p0: String?): Boolean {
                return false
            }
            override fun onQueryTextChange(newText: String?): Boolean {
                adapter.filter("$newText")
                return true
            }
        })

        newChatDialog.show()
    }

    private fun initToolbar(toolbar: androidx.appcompat.widget.Toolbar){
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setTitle(R.string.experimental)

        toolbar.menu.findItem(R.id.main_menu_clear).isVisible = true
        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.main_menu_public_chat_button -> {
                    val fragmentManager = this.parentFragmentManager
                    fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ChatFragment()).commit()
                    true
                }
                R.id.main_menu_clear ->{
                    LocalDB.clearFriends()
                    //BeaconV2.stop()
                    Beacon.stop()

                    activity!!.supportFragmentManager.beginTransaction()
                        .detach(this)
                        .commit()
                    activity!!.supportFragmentManager.beginTransaction()
                        .attach(this)
                        .commit()
                    true
                }
                R.id.main_menu_settings_button ->{
                    val fragmentManager = this.parentFragmentManager
                    fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, FunctionalitySettingsFragment()).commit()
                    true
                }
                R.id.main_menu_edit_profile_button -> {
                    val fragmentManager = this.parentFragmentManager
                    fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ProfileFragment()).commit()
                    true
                }
                R.id.main_menu_experimental->{
                    val fragmentManager = this.parentFragmentManager
                    fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, TestFragment2()).commit()
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

}