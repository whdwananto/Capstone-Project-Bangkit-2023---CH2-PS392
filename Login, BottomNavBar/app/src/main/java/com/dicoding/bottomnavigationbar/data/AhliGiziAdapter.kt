package com.dicoding.bottomnavigationbar.data


import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.dicoding.bottomnavigationbar.R
import com.bumptech.glide.Glide

class AhliGiziAdapter(private val listAhliGizi: ArrayList<AhliGizi>) : RecyclerView.Adapter<AhliGiziAdapter.ListViewHolder>() {
    private lateinit var onItemClickCallback: OnItemClickCallback

    class ListViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvName: TextView = itemView.findViewById(R.id.tv_nama_dokter)
        val tvRumahSakit: TextView = itemView.findViewById(R.id.tv_rs)
        val tvNomor: TextView = itemView.findViewById(R.id.tv_telp)
        val tvJadwal: TextView = itemView.findViewById(R.id.tv_jadwal)
        val imgPhoto: ImageView = itemView.findViewById(R.id.iv_dokter)

        val buttonRead: Button = itemView.findViewById(R.id.button_read)

    }

    fun setOnItemClickCallback(onItemClickCallback: OnItemClickCallback) {
        this.onItemClickCallback = onItemClickCallback}
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListViewHolder {
        val view: View = LayoutInflater.from(parent.context).inflate(R.layout.item_ahligizi, parent, false)
        return ListViewHolder(view)
    }

    override fun onBindViewHolder(holder: ListViewHolder, position: Int) {
        val (name,rumahsakit,nomor,jadwal,photo) = listAhliGizi[position]
        Glide.with(holder.itemView.context)
            .load(photo) // URL Gambar
            .into(holder.imgPhoto) // imageView mana yang akan diterapkan
        holder.tvName.text = name
        holder.tvRumahSakit.text = rumahsakit
        holder.tvNomor.text = nomor
        holder.tvJadwal.text = jadwal
        holder.itemView.setOnClickListener { onItemClickCallback.onItemClicked(listAhliGizi[holder.adapterPosition]) }
        holder.buttonRead.setOnClickListener {val intent = Intent(Intent.ACTION_VIEW)
            intent.data = Uri.parse("https://wa.me/$nomor")
            holder.itemView.context.startActivity(intent)
        }
    }
    override fun getItemCount(): Int = listAhliGizi.size
    interface OnItemClickCallback {
        fun onItemClicked(data: AhliGizi)
    }

}