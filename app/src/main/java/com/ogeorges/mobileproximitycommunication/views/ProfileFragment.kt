package com.ogeorges.mobileproximitycommunication.views

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import com.ogeorges.mobileproximitycommunication.databinding.FragmentProfileBinding
import com.ogeorges.mobileproximitycommunication.vmodels.ProfileViewModel

class ProfileFragment : Fragment() {

    private lateinit var binding: FragmentProfileBinding
    private lateinit var viewModel: ProfileViewModel

    val rfaInit = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()) { result ->
        viewModel.setAvatarImageImage(result, binding.avatarImage)
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentProfileBinding.inflate(inflater, container, false)
        viewModel = ProfileViewModel(activity!!.getSharedPreferences("userPreferences", Context.MODE_PRIVATE), context!!)
        binding.viewModel = viewModel

        return binding.root
    }



}