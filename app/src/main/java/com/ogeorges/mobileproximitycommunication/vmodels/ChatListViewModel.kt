package com.ogeorges.mobileproximitycommunication.vmodels

import android.app.Dialog
import android.view.View
import android.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.findFragment
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.adapters.UserToNewChatButtonAdapter
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.models.Friend
import com.ogeorges.mobileproximitycommunication.models.User
import com.ogeorges.mobileproximitycommunication.views.ChatFragment
import com.ogeorges.mobileproximitycommunication.views.ChatListFragment
import com.ogeorges.mobileproximitycommunication.views.FunctionalitySettingsFragment
import com.ogeorges.mobileproximitycommunication.views.ProfileFragment
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.*

class ChatListViewModel : ViewModel() {

    inner class BeaconObserver(private val adapter: UserToNewChatButtonAdapter): Observer {
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

            Beacon.observedReachablePeopleSize = currentSize
        }

    }


    fun newChatActionButtonAction(view: View){
        val fragment = view.findFragment<ChatListFragment>()
        val newChatDialog = Dialog(fragment.activity!!)

        newChatDialog.setCancelable(true)

        newChatDialog.setContentView(R.layout.new_chat_with)

        val newChatWithPeopleList: RecyclerView = newChatDialog.findViewById(R.id.new_chat_with_people_list)

        val manager = LinearLayoutManager(newChatWithPeopleList.context)
        manager.orientation = LinearLayoutManager.VERTICAL
        newChatWithPeopleList.layoutManager = manager

        val adapter = UserToNewChatButtonAdapter(ArrayList(Beacon.readReachablePeople())) {
            val f = Friend(it, Friend.PENDING)
            f.update(it)
            val avatarImgPath = fragment.activity!!.filesDir.path +  "/TEMPAvatarOf${f.stringid}"
            val avatarImg = File(avatarImgPath)
            if(avatarImg.exists()){
                runBlocking {
                    val inputStream = FileInputStream(avatarImg)
                    val outputStream = FileOutputStream(File(fragment.activity!!.filesDir.path +  "/AvatarOf${f.id}.jpeg"))
                    outputStream.write(inputStream.readBytes())
                    outputStream.close()
                    inputStream.close()
                    LocalDB.modifyFriend(f.toDBFriend())
                }
            }

            if (LocalDB.readFriendList()?.find { alreadyFriend ->
                alreadyFriend.hashCode() == f.id } != null) return@UserToNewChatButtonAdapter
            Beacon.sendHandShakeMessage(it.stringid, Beacon.PAYLOAD_HANDSHAKE_REQUEST)
            LocalDB.addFriend(f.toDBFriend())
            newChatDialog.dismiss()
        }
        newChatWithPeopleList.adapter = adapter

        val beaconObserver = BeaconObserver(adapter)
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

    fun initToolbar(toolbar: Toolbar, fragment: ChatListFragment){
        toolbar.inflateMenu(R.menu.main_menu)
        toolbar.setTitle(R.string.chats)

        toolbar.menu.findItem(R.id.main_menu_clear).isVisible = true
        toolbar.menu.findItem(R.id.main_menu_refresh).isVisible = true

        toolbar.setOnMenuItemClickListener {
            when(it.itemId){
                R.id.main_menu_public_chat_button -> {
                    val fragmentManager = fragment.parentFragmentManager
                    val transaction = fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ChatFragment())
                    transaction.addToBackStack(null)
                    transaction.commit()
                    true
                }
                R.id.main_menu_refresh ->{
                    Beacon.stop()
                    Beacon.start()
                    fragment.binding.activeChatsList.adapter?.notifyDataSetChanged()
                    true
                }
                R.id.main_menu_clear ->{
                    LocalDB.clearFriends()
                    fragment.activity!!.supportFragmentManager.beginTransaction()
                        .detach(fragment)
                        .commit()
                    fragment.activity!!.supportFragmentManager.beginTransaction()
                        .attach(fragment)
                        .commit()
                    true
                }
                R.id.main_menu_settings_button ->{
                    val fragmentManager = fragment.parentFragmentManager
                    val transaction = fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, FunctionalitySettingsFragment())
                    transaction.addToBackStack(null)
                    transaction.commit()

                    true
                }
                R.id.main_menu_edit_profile_button -> {
                    val fragmentManager = fragment.parentFragmentManager
                    val transaction = fragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ProfileFragment())
                    transaction.addToBackStack(null)
                    transaction.commit()
                    true
                }
                else -> {
                    false
                }
            }
        }
    }

}
