package com.example.music1.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music1.R;
import com.example.music1.models.Cancion;
import com.example.music1.utils.TagEditorManager;

import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.ViewHolder> {

    private static final String TAG = "PlaylistAdapter";
    private List<Cancion> canciones;
    private OnItemClickListener listener;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public PlaylistAdapter(List<Cancion> canciones, OnItemClickListener listener) {
        this.canciones = canciones;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cancion, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Cancion cancion = canciones.get(position);
        holder.bind(cancion, listener, context);
    }

    @Override
    public int getItemCount() {
        return canciones.size();
    }

    public void updateList(List<Cancion> nuevasCanciones) {
        this.canciones = nuevasCanciones;
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivAlbum;
        TextView tvTitulo;
        TextView tvArtista;
        TextView tvDuracion;

        ViewHolder(View itemView) {
            super(itemView);
            ivAlbum = itemView.findViewById(R.id.ivAlbum);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvArtista = itemView.findViewById(R.id.tvArtista);
            tvDuracion = itemView.findViewById(R.id.tvDuracion);
        }

        void bind(final Cancion cancion, final OnItemClickListener listener, Context context) {
            TagEditorManager manager = TagEditorManager.getInstance(context);

            String tituloMostrable = manager.getTitulo(
                    cancion.getId(), cancion.getUriString(), cancion.getTitulo());
            String artistaMostrable = manager.getArtista(
                    cancion.getId(), cancion.getUriString(), cancion.getArtista());

            ivAlbum.setImageResource(cancion.getImagenAlbum());
            tvTitulo.setText(tituloMostrable);
            tvArtista.setText(artistaMostrable);
            tvDuracion.setText(cancion.getDuracionFormateada());

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                listener.onItemClick(position);
            });
        }
    }
}