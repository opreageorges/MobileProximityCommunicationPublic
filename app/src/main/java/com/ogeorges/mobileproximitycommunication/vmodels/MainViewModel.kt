package com.ogeorges.mobileproximitycommunication.vmodels

import android.content.Context
import android.content.SharedPreferences
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.ViewModel
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.models.Beacon
import com.ogeorges.mobileproximitycommunication.localdatabase.LocalDB
import com.ogeorges.mobileproximitycommunication.models.Cryptor
import com.ogeorges.mobileproximitycommunication.views.ChatListFragment
import com.ogeorges.mobileproximitycommunication.views.ProfileFragment
import java.io.File

class MainViewModel(private val sharedPreferences: SharedPreferences, context: Context): ViewModel() {

    private val filesPath: String

     init{
         Cryptor.initKeys(context)
         LocalDB.init(context)
         filesPath = context.filesDir.path
    }

    fun setInitialFragment(supportFragmentManager: FragmentManager){
        if(sharedPreferences.getBoolean("isFirstTimeOpen", true)){
            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ProfileFragment()).commit()
        }
        else{
            supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ChatListFragment()).commit()
        }
    }

    fun stop(){
        Beacon.stop()
        val storageDir = File(filesPath)
        for (file in storageDir.listFiles()!!) {
            if ("TEMP" in file.path) file.delete()
        }
    }
}