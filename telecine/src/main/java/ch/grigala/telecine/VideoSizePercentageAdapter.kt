package ch.grigala.telecine

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

internal class VideoSizePercentageAdapter(context: Context) : BaseAdapter() {

    private val inflater: LayoutInflater

    init {
        inflater = LayoutInflater.from(context)
    }

    override fun getCount(): Int {
        return 3
    }

    override fun getItem(position: Int): Int {
        when (position) {
            0 -> return 100
            1 -> return 75
            2 -> return 50
            else -> throw IllegalArgumentException("Unknown position: $position")
        }
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun getView(position: Int, convertView: View, parent: ViewGroup): View {
        var tv: TextView? = convertView as TextView
        if (tv == null) {
            tv = inflater.inflate(android.R.layout.simple_spinner_dropdown_item, parent, false) as TextView
        }

        tv.text = getItem(position).toString() + "%"

        return tv
    }

    companion object {
        fun getSelectedPosition(value: Int): Int {
            when (value) {
                100 -> return 0
                75 -> return 1
                50 -> return 2
                else -> return 0
            }
        }
    }
}