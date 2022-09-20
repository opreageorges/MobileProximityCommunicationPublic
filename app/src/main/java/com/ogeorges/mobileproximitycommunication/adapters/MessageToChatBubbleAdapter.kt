package com.ogeorges.mobileproximitycommunication.adapters

import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.ogeorges.mobileproximitycommunication.R
import com.ogeorges.mobileproximitycommunication.models.ChatMessage

class MessageToChatBubbleAdapter(private val dataSet: MutableSet<ChatMessage>) : RecyclerView.Adapter<MessageToChatBubbleAdapter.ViewHolder>()  {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val message: TextView
        val sender: TextView
        val messageHolder: ConstraintLayout
        init {
            message = view.findViewById(R.id.chat_bubble_message)
            sender = view.findViewById(R.id.chat_bubble_sender_name)
            messageHolder = view.findViewById(R.id.chat_bubble_message_holder)
        }
    }

    fun add(message: ChatMessage){
        dataSet.add(message)
        val index = dataSet.indexOf(message)
        notifyItemInserted(index)
    }

    fun clear(){
        dataSet.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.chat_bubble, parent, false)

        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val arrayDataSet = this.dataSet.toTypedArray()

        holder.message.text = arrayDataSet[position].body
        holder.sender.text = arrayDataSet[position].sender

        if (arrayDataSet[position].sender_type == ChatMessage.SENDER_TYPE_ME){
            val params = FrameLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                gravity = Gravity.START
                holder.sender.text = "Me"
            }

            holder.messageHolder.layoutParams = params
        }

    }

    override fun getItemCount(): Int = dataSet.size
}