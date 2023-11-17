package com.example.walkingnavigator

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.yandex.mapkit.search.SuggestItem

class SearchAdapter(val onItemClicked: (String, latitude: Double?,longitude:Double?) -> Unit) :
    RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    private var listSearch: List<SuggestItem> = emptyList()


    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tv_title)
        val tvSubtitle: TextView = view.findViewById(R.id.tv_subtitle)

    }

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.item_search, viewGroup, false)
        return ViewHolder(view)
    }


    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        viewHolder.itemView.setOnClickListener {
            val text = it.findViewById<TextView>(R.id.tv_title).text.toString()
            onItemClicked(text,listSearch[position].center?.latitude,listSearch[position].center?.longitude )
        }
        viewHolder.tvTitle.text = listSearch[position].title.text
        viewHolder.tvSubtitle.text = listSearch[position].subtitle?.text



    }

    override fun getItemCount() = listSearch.size

    fun setData(listSearch: MutableList<SuggestItem>) {
        this.listSearch = listSearch
        notifyDataSetChanged()
    }


}