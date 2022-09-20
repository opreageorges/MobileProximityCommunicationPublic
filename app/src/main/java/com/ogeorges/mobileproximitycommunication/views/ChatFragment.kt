package com.ogeorges.mobileproximitycommunication.views

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.ogeorges.mobileproximitycommunication.databinding.FragmentChatBinding
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.localdatabase.entities.DBFriend
import com.ogeorges.mobileproximitycommunication.models.User
import com.ogeorges.mobileproximitycommunication.vmodels.ChatViewModel
import java.util.*

class ChatFragment(val friend: DBFriend?) : Fragment() {

    constructor(): this(null)

    lateinit var binding: FragmentChatBinding
    private lateinit var viewModel: ChatViewModel
    private lateinit var beaconObserver: BeaconObserver

    fun verifyUserIsAvailable(){
        if (Beacon.readReachablePeople().find { it.id == friend?.hashCode() } != null){
            binding.imageButton.clearColorFilter()
            binding.imageButton.setColorFilter(Color.GREEN)
        }
        else{
            binding.imageButton.clearColorFilter()
            binding.imageButton.setColorFilter(Color.RED)
        }
    }

    inner class BeaconObserver: Observer{
        override fun update(p0: Observable?, p1: Any?) {
            if(p1 == null || p1 !is User || p1.id != friend.hashCode()) return

            verifyUserIsAvailable()

        }

    }

    override fun onResume() {
        if (friend != null)  verifyUserIsAvailable()
        super.onResume()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentChatBinding.inflate(inflater, container, false)
        viewModel = ChatViewModel(friend)
        viewModel.initToolBar(binding.chatToolbar)
        if (friend != null) {
            beaconObserver = BeaconObserver()
            Beacon.addObserver(beaconObserver)
        }
        binding.viewModel = viewModel
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewModel.initMessagesArea(binding.chatMessagesArea)
        super.onViewCreated(view, savedInstanceState)
    }

    override fun onDestroy() {
        if (friend != null) Beacon.deleteObserver(beaconObserver)
        viewModel.stop()
        super.onDestroy()
    }
}