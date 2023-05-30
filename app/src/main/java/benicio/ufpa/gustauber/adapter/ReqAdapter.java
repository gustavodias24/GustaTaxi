package benicio.ufpa.gustauber.adapter;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.view.menu.MenuView;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;

import benicio.ufpa.gustauber.Model.ReqModel;
import benicio.ufpa.gustauber.R;
import benicio.ufpa.gustauber.databinding.RequisicaoLayoutBinding;

public class ReqAdapter extends RecyclerView.Adapter<ReqAdapter.MyViewHolder> {

    List<ReqModel> listReq;
    Context c;
    String id_usuario;
    Activity a;

    private DatabaseReference refRequisicoes = FirebaseDatabase.getInstance().getReference("requisicoes");

    public ReqAdapter(List<ReqModel> listReq, Context c, String id_usuario, Activity a) {
        this.listReq = listReq;
        this.c = c;
        this.id_usuario = id_usuario;
        this.a = a;
    }

    @NonNull
    @Override
    public MyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.requisicao_layout, parent, false);
        return new MyViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull MyViewHolder holder, int position) {

            ReqModel req = listReq.get(position);

            holder.textNomeCliente.setText(String.format("Corrida de %s.", req.getNomeCliente()));

            holder.textValor.setText(String.format("Valor: R$ %s", req.getValor()));



            refRequisicoes.child(id_usuario).child(req.getId()).child("status").addValueEventListener(
                    new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String msgStatus = "";
                            int corMsg = Color.BLACK;
                            switch (snapshot.getValue(Integer.class)){
                                case 0:
                                    msgStatus = "Cancelado.";
                                    corMsg = Color.RED;
                                    holder.layoutBtn.setVisibility(View.GONE);
                                    holder.btn_ver_caminho.setVisibility(View.GONE);
                                    holder.btn_finalizar.setVisibility(View.GONE);
                                    break;
                                case 1:
                                    msgStatus = "Em andamento.";
                                    corMsg = Color.YELLOW;
                                    holder.layoutBtn.setVisibility(View.GONE);
                                    holder.btn_finalizar.setVisibility(View.VISIBLE);
                                    holder.btn_ver_caminho.setVisibility(View.VISIBLE);
                                    break;
                                case 2:
                                    msgStatus = "Concluído.";
                                    corMsg = Color.GREEN;
                                    holder.layoutBtn.setVisibility(View.GONE);
                                    holder.btn_ver_caminho.setVisibility(View.GONE);
                                    holder.btn_finalizar.setVisibility(View.GONE);
                                    break;
                                case 3:
                                    msgStatus = "Aguardando.";
                                    holder.layoutBtn.setVisibility(View.VISIBLE);
                                    corMsg = Color.BLUE;
                                    break;
                                default:
                                    msgStatus = "Indefinido.";
                                    holder.layoutBtn.setVisibility(View.GONE);
                                    break;
                            }

                            holder.textStatus.setText(String.format("Status: %s", msgStatus));
                            holder.textStatus.setTextColor(corMsg);
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    }
            );


            holder.btn_finalizar.setOnClickListener( viewfim -> {
                req.setStatus(2);
                refRequisicoes.child(id_usuario).child(req.getId()).setValue(req);
                holder.btn_finalizar.setVisibility(View.GONE);
                holder.btn_ver_caminho.setVisibility(View.GONE);
                Toast.makeText(c, "Corrida concluída", Toast.LENGTH_SHORT).show();
            });


            holder.btn_ver_caminho.setOnClickListener( viewVerCaminho -> {
                Toast.makeText(c, "Siga esse destino.", Toast.LENGTH_SHORT).show();
                Uri gmmIntentUri = Uri.parse(
                       String.format("google.navigation:q=%s,%s&mode=d", req.getLatDest(), req.getLongDest())
                );
                Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                mapIntent.setPackage("com.google.android.apps.maps");
                a.startActivity(mapIntent);
            });


            holder.btnAceitar.setOnClickListener( viewAcc -> {
                req.setStatus(1);
                refRequisicoes.child(id_usuario).child(req.getId()).setValue(req);
                holder.layoutBtn.setVisibility(View.GONE);
                Toast.makeText(c, "Corrida aceita", Toast.LENGTH_SHORT).show();
            });

            holder.btnCancelar.setOnClickListener( viewCancel -> {
                req.setStatus(0);
                refRequisicoes.child(id_usuario).child(req.getId()).setValue(req);
                holder.layoutBtn.setVisibility(View.GONE);
                Toast.makeText(c, "Corrida cancelada", Toast.LENGTH_SHORT).show();

            });

    }

    @Override
    public int getItemCount() {
        return listReq.size();
    }

    public class MyViewHolder extends RecyclerView.ViewHolder {

        TextView textNomeCliente, textStatus, textValor;
        LinearLayout layoutBtn;
        ImageButton btnCancelar, btnAceitar;
        Button btn_finalizar, btn_ver_caminho;

        public MyViewHolder(@NonNull View itemView) {
            super(itemView);

            textNomeCliente = itemView.findViewById(R.id.textCorridaDe);
            textStatus = itemView.findViewById(R.id.textStatus);
            textValor = itemView.findViewById(R.id.textValor);
            layoutBtn = itemView.findViewById(R.id.layoutBtns);
            btnCancelar = itemView.findViewById(R.id.btnCancelar);
            btnAceitar = itemView.findViewById(R.id.btnAceitar);
            btn_finalizar = itemView.findViewById(R.id.btnFinalizar);
            btn_ver_caminho = itemView.findViewById(R.id.btnVerCaminho);
        }
    }
}
