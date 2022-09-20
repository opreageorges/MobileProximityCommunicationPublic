package com.ogeorges.mobileproximitycommunication.vmodels

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.view.View
import android.widget.ImageButton
import androidx.activity.result.ActivityResult
import androidx.core.graphics.scale
import androidx.databinding.BaseObservable
import androidx.databinding.Bindable
import androidx.databinding.library.baseAdapters.BR
import androidx.fragment.app.findFragment
import com.google.android.material.internal.ContextUtils.getActivity
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.models.User
import com.ogeorges.mobileproximitycommunication.views.ChatListFragment
import com.ogeorges.mobileproximitycommunication.views.ProfileFragment
import java.io.File
import java.io.FileOutputStream

class ProfileViewModel(private val sharedPreferences: SharedPreferences, private val context: Context) : BaseObservable() {

    var user: User = User.getSelf(context)

    @Bindable
    fun setUserRealName(realName: String){
        user.realname = realName.replace("\n", "")
        notifyPropertyChanged(BR.userRealName)
    }

    fun getUserRealName(): String{
        return user.realname
    }

    @Bindable
    fun setUserUserName(username: String){
        user.username = username.replace("\n", "")
        notifyPropertyChanged(BR.userUserName)
    }

    fun getUserUserName(): String{
        return user.username

    }

    fun finishProfile(view: View){
        sharedPreferences.edit().putString("myUserName", user.username).commit()
        sharedPreferences.edit().putString("myRealName", user.realname).commit()
        sharedPreferences.edit().putString("myAvatarPath", user.avatarimg.toString()).commit()
        sharedPreferences.edit().putBoolean("isFirstTimeOpen", false).commit()
        val curFragment = view.findFragment<ProfileFragment>()
        curFragment.activity!!.supportFragmentManager.beginTransaction().replace(R.id.fragmentContainerView, ChatListFragment()).commit()

    }

    fun setAvatarImageImage(result: ActivityResult, avatarImage: ImageButton){
        if (result.resultCode == Activity.RESULT_OK){
            val file = File(context.filesDir.path + "/" + "myAvatar.jpeg")

            user.avatarimg = context.filesDir.path + "/" + "myAvatar.jpeg"

            val outputStream = FileOutputStream(file)
            val input = getActivity(context)!!.contentResolver.openInputStream(result.data!!.data!!)!!
            var initialImage = BitmapFactory.decodeStream(input)
            input.close()

            initialImage = initialImage.scale(200, 200, true)
            initialImage.compress(Bitmap.CompressFormat.JPEG,100, outputStream)

            avatarImage.setImageURI(result.data!!.data!!)
        }
    }

    fun avatarImageAction(curView: View){
        val curFragment = curView.findFragment<ProfileFragment>()
        curFragment.rfaInit
        val i = Intent(Intent.ACTION_GET_CONTENT)
        i.type = "image/*"
        curFragment.rfaInit.launch(i)

    }

}