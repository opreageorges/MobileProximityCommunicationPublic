package com.ogeorges.mobileproximitycommunication.models

import android.content.Context
import com.google.gson.Gson
import java.io.File
import java.security.PublicKey

open class User(var username: String = "",
                var realname: String = "",
                var avatarimg: String?,
                var publicKeyString: String,
                ){

    companion object{
        fun getSelf(context: Context): User{
            val sharedPreferences = context.getSharedPreferences("userPreferences", Context.MODE_PRIVATE)
            return User(
                    sharedPreferences.getString("myUserName", "")!!,
                    sharedPreferences.getString("myRealName", "")!!,
                    sharedPreferences.getString("myAvatarPath", "")!!,
                    Cryptor.byteArrayToStringLossless(Cryptor.getMessagesPublicKey().encoded))

        }
        fun fromJson(string: String):User{
            return Gson().fromJson(string, User::class.java)
        }

        fun toJson(user: User):String{
            return Gson().toJson(user)
        }
    }
    var id: Int
    var stringid: String = ""

    init {

        if (!(File(avatarimg.toString()).exists())){
            avatarimg = null
        }
        id = this.hashCode()
    }

    fun getPublicKey(): PublicKey{
        return Cryptor.stringToPublicKey(Cryptor.stringToByteArrayLossless(publicKeyString))
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as User

        if (username != other.username) return false
        if (realname != other.realname) return false
        if (avatarimg != other.avatarimg) return false
        if (publicKeyString != other.publicKeyString) return false

        return true
    }

    override fun hashCode(): Int {
        var result = realname.hashCode()
        result = 31 * result + publicKeyString.hashCode()
        return result
    }

}