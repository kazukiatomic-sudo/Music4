package com.example.music1.adapters;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.music1.R;
import com.example.music1.models.Favorito;
import com.example.music1.utils.FavoritosManager;
import com.example.music1.utils.TagEditorManager;

import java.util.List;

public class FavoritoAdapter extends RecyclerView.Adapter<FavoritoAdapter.ViewHolder> {

    private static final String TAG = "FavoritoAdapter";
    private List<Favorito> favoritos;
    private OnItemClickListener listener;
    private FavoritosManager favoritosManager;
    private Context context;

    public interface OnItemClickListener {
        void onItemClick(Favorito favorito);
        void onEliminarClick(Favorito favorito);
    }

    public FavoritoAdapter(List<Favorito> favoritos, OnItemClickListener listener) {
        this.favoritos = favoritos;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        this.context = parent.getContext();
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorito, parent, false);
        favoritosManager = FavoritosManager.getInstance(parent.getContext());
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (position >= 0 && position < favoritos.size()) {
            Favorito favorito = favoritos.get(position);
            holder.bind(favorito, listener, context);
        }
    }

    @Override
    public int getItemCount() {
        return favoritos.size();
    }

    public void updateList(List<Favorito> nuevosFavoritos) {
        this.favoritos = nuevosFavoritos;
        notifyDataSetChanged();
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcono;
        TextView tvTitulo;
        TextView tvArtista;
        TextView tvDuracion;
        ImageButton btnEliminar;

        ViewHolder(View itemView) {
            super(itemView);
            ivIcono = itemView.findViewById(R.id.ivIcono);
            tvTitulo = itemView.findViewById(R.id.tvTitulo);
            tvArtista = itemView.findViewById(R.id.tvArtista);
            tvDuracion = itemView.findViewById(R.id.tvDuracion);
            btnEliminar = itemView.findViewById(R.id.btnEliminar);
        }

        void bind(final Favorito favorito, final OnItemClickListener listener, Context context) {
            TagEditorManager manager = TagEditorManager.getInstance(context);

            String tituloMostrable = manager.getTitulo(
                    favorito.getId(), favorito.getUri(), favorito.getTitulo());
            String artistaMostrable = manager.getArtista(
                    favorito.getId(), favorito.getUri(), favorito.getArtista());

            tvTitulo.setText(tituloMostrable);
            tvArtista.setText(artistaMostrable);
            tvDuracion.setText(favorito.getDuracionFormateada());
            ivIcono.setImageResource(R.drawable.ic_favorite);

            itemView.setOnClickListener(v -> {
                listener.onItemClick(favorito);
            });

            btnEliminar.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < favoritos.size()) {
                    favoritosManager.quitarFavorito(favorito.getId(), favorito.getUri());
                    favoritos.remove(position);
                    notifyItemRemoved(position);
                    listener.onEliminarClick(favorito);
                }
            });
        }
    }
}