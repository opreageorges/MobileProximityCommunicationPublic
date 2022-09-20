

package com.ogeorges.mobileproximitycommunication.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.models.User

class UserToNewChatButtonAdapter(private var dataSet: ArrayList<User>,
                                    private val clickListener:(User)-> Unit) :
                                    RecyclerView.Adapter<UserToNewChatButtonAdapter.ViewHolder>() {

    @Suppress("UNCHECKED_CAST")
    private val initialDataSet: ArrayList<User> = dataSet.clone() as ArrayList<User>
    private var lastFilter: String = ""

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val newChatWithButtonUsername: TextView
        val newChatWithButtonIdentifier: TextView

        init {
            newChatWithButtonUsername = view.findViewById(R.id.new_chat_with_button_username)
            newChatWithButtonIdentifier = view.findViewById(R.id.new_chat_with_button_identifier)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.new_chat_with_button, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.newChatWithButtonUsername.text = this.dataSet[position].username
        holder.newChatWithButtonIdentifier.text = this.dataSet[position].id.toString()
        holder.itemView.setOnClickListener {
            clickListener(this.dataSet[position])
        }
    }

    fun add(u: User){
        initialDataSet.add(u)
        filter(lastFilter)
    }

    fun remove(u: User){
        initialDataSet.remove(u)
        filter(lastFilter)
    }

    fun filter(string: String){
        lastFilter = string
        dataSet = initialDataSet.filter {
            it.username.contains(string)
        } as ArrayList<User>
        notifyDataSetChanged()
    }



    override fun getItemCount(): Int = dataSet.size
}