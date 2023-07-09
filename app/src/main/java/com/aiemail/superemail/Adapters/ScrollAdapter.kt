package com.aiemail.superemail.Adapters


import android.app.Activity
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.aiemail.superemail.Activities.ComposeActivity
import com.aiemail.superemail.R
import com.aiemail.superemail.Viewholders.MailSection
import com.aiemail.superemail.Models.AllMails
import com.aiemail.superemail.Models.Email
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.snackbar.Snackbar


class ScrollAdapter(
    context: Activity,
    sourceList: ArrayList<Email>,
    clickListener: ClickListenerData,
    var showselect: (Boolean) -> Unit
) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var isEnable = false
    private var fromlongclick = false
    private val itemselectedList = mutableListOf<Email>()
    private var checkedPosition = 0
    val CITY_TYPE = 0
    val COUNTRY_TYPE = 1
    private var context: Activity
    private var sourceList: ArrayList<Email> = arrayListOf()
    private var sourceListselect: ArrayList<Email> = arrayListOf()
    private lateinit var mRecentlyDeletedItem: Email
    var mRecentlyDeletedItemPosition = 0
    private val clickListener: ClickListenerData

    init {

        Log.d("TAG", "NewsAdapter: " + sourceList.size)
        this.context = context
        this.sourceList = sourceList
        this.clickListener = clickListener

    }

    fun deleteItem(position: Int, a: Activity) {
        mRecentlyDeletedItem = sourceList!![position]
        mRecentlyDeletedItemPosition = position
        sourceList.removeAt(position)
        notifyItemRemoved(position)
        showUndoSnackbar(a)
    }

    var isselectall: Boolean = false
    fun getAllList(select: Boolean) {
        isselectall = select
        showselect(select)
        isEnable = select
        notifyDataSetChanged()

    }

    fun showUndoSnackbar(activity: Activity) {
        val view = activity.findViewById<View>(R.id.appbar)
        val snackbar = Snackbar.make(
            view, R.string.snack_bar_text,
            Snackbar.LENGTH_LONG
        )
        snackbar.setAction(R.string.snack_bar_undo) { v: View? -> undoDelete() }
        snackbar.show()
    }

    private fun undoDelete() {
        sourceList.add(
            mRecentlyDeletedItemPosition,
            mRecentlyDeletedItem
        )
        notifyItemInserted(mRecentlyDeletedItemPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view: View

        //  return null
        view = LayoutInflater.from(parent.context)
            .inflate(R.layout.layout_scroll, parent, false)
        return SectionItemVH(view)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (sourceList.size != 0) {
            val `object` = sourceList!![position]

            if (`object` != null) {
                when (`object`.type) {

                    COUNTRY_TYPE -> {
                        sourceListselect.add(`object`)
                        if (!`object`.title.equals(null))
                            (holder as SectionItemVH?)!!.textView.text = `object`.sender!!
//                    (holder as SectionItemVH?)!!.txtSourceName.text = `object`.content
                        (holder as SectionItemVH?)!!.txtsubtitle.text = `object`.senderEmail
                        (holder as SectionItemVH?)!!.subject_text.text = `object`.author
                        Glide.with(context)
                            .load(`object`.url)
                            .centerCrop()
                            .placeholder(R.drawable.pic4)
                            .apply(RequestOptions().override(100, 100))
                            .into((holder as SectionItemVH?)!!.img_thumbnail)


                        (holder as SectionItemVH).accept.setOnClickListener {

                        }

                        (holder as SectionItemVH).block.setOnClickListener {

                        }

                        (holder as SectionItemVH).outer.setOnLongClickListener {

                            selectItem(holder, `object`, position)
                            var index = getCategoryPos(`object`)
                            showselect(true)
                            isEnable = true
                            fromlongclick = true
                            notifyDataSetChanged()
                            true

                        }
                        (holder as SectionItemVH).accept.setOnClickListener { removeItem(position) }
                        (holder as SectionItemVH). block.setOnClickListener { removeItem(position) }
                        (holder as SectionItemVH).outer.setOnClickListener {
                            Log.d(
                                "TAG",
                                "onBindViewHolder: " + itemselectedList.size + ">" + ">" + position + ">>" + `object`.title
                            )
                            var index = getCategoryPos(`object`)
                            if (itemselectedList.size != 0)
                                Log.d(
                                    "TAG",
                                    "onBindViewHolder: " + itemselectedList.size + ">" + ">" + index + ">>" + `object`.title
                                )

                            if (itemselectedList.contains(`object`)) {
                                itemselectedList.removeAt(index)


                                `object`.isselected = false
                                if (itemselectedList.isEmpty()) {
                                    showselect(false)
                                    isEnable = false
                                }
                                Log.d(
                                    "TAG",
                                    "onBindViewHolder:itemselectedlist " + itemselectedList.size
                                )
                                if (itemselectedList.size == 0) {
                                    notifyDataSetChanged()
                                }


                            } else {
                                if (isEnable) {
                                    isEnable = false

                                    selectItem(holder, `object`, position)

                                    showselect(true)
                                } else {
                                    Log.d("TAG", "onBindViewHolder: " + `object`)
                                    clickListener.onItemRootViewClicked(`object`, position)

                                }


                            }
                        }
                        if (isEnable && isselectall) {

                            holder.img_thumbnail.setImageResource(R.drawable.ic_unselect)

                        } else {
                            if (isEnable) {
                                //holder.selection.visibility = View.VISIBLE
                                // holder.img_thumbnail.setImageResource(R.drawable.ic_unselect)
                                if (fromlongclick) {
                                    holder.img_thumbnail.setImageResource(R.drawable.ic_unselect)
                                }
                            } else {

                                holder.img_thumbnail.visibility = View.VISIBLE
                            }

                        }


                    }


                }

            }
        }


    }

    private fun getCategoryPos(category: Email): Int {
        return itemselectedList.indexOf(category)
    }

    private fun selectItem(holder: SectionItemVH, article: Email, position: Int) {
        isEnable = true
        itemselectedList.add(article)
        article.isselected = true

        Log.d("TAG", "onBindViewHolder: " + article.id)


    }

    override fun getItemCount(): Int {
        return sourceList.size ?: 0
    }

    override fun getItemViewType(position: Int): Int {
        if (sourceList != null && sourceList.size != 0) {
            val `object` = sourceList[position]
            if (`object` != null) {
                return `object`.type
            }
        }
        return 0
    }

    fun adddata(articles: MutableList<Email>?) {
        val diffResult = DiffUtil.calculateDiff(
            ItemDiffCallback(
                sourceList,
                articles!!
            )
        )
        sourceList = articles as ArrayList<Email>
        diffResult.dispatchUpdatesTo(this)

    }

    fun spiltString(someText: String): String {
        var spiltString = someText.split("<")
        Log.d("TAG", "spiltString: " + spiltString)
        for (substring in spiltString) {
            if (!substring.isEmpty()) {
                return substring
            }
        }
        return ""
    }

    internal inner class SectionItemVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var textView: TextView
        var img_thumbnail: ImageView


        val txtsubtitle: TextView
        val subject_text: TextView
        val outer: LinearLayout
        val accept: TextView
        val block: TextView

        init {

            outer = itemView.findViewById(R.id.outer_layout)
            textView = itemView.findViewById(R.id.txt_title)
            img_thumbnail = itemView.findViewById(R.id.img_thumbnail)
            accept = itemView.findViewById(R.id.accept)
            block = itemView.findViewById(R.id.block)


            txtsubtitle = itemView.findViewById(R.id.email)
            subject_text = itemView.findViewById(R.id.subject_text)
            // swipe = itemView.findViewById(R.id.swipe_layoutmain)
            itemView.setLongClickable(true);


            itemView.setOnClickListener {

                if (checkedPosition !== adapterPosition) {
                    notifyItemChanged(checkedPosition)
                    checkedPosition = adapterPosition
                }
            }

        }


    }
    fun removeItem(position: Int): Email? {
        var item: Email? = null
        try {
            item = sourceList.get(position)
            sourceList.removeAt(position)
            notifyItemRemoved(position)
        } catch (e: java.lang.Exception) {
            Log.e("TAG", e.message!!)
        }
        return item
    }

    internal inner class SectionHeaderVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var tvHeader: TextView

        init {
            tvHeader = itemView.findViewById(R.id.txt_source_name)
        }
    }

    interface ClickListenerData {
        fun onItemRootViewClicked(
            section: Email,
            itemAdapterPosition: Int
        )


    }

    private class ItemDiffCallback(
        private val oldList: List<Email>,
        private val newList: List<Email>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size

        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition] == newList[newItemPosition]
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].msgid == newList[newItemPosition].msgid
        }
    }
}