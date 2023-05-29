package benicio.ufpa.gustauber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.sql.Array;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import benicio.ufpa.gustauber.Model.ReqModel;
import benicio.ufpa.gustauber.R;
import benicio.ufpa.gustauber.adapter.ReqAdapter;
import benicio.ufpa.gustauber.databinding.ActivityRequisicoesBinding;
import benicio.ufpa.gustauber.databinding.ChamadaLayoutBinding;
import benicio.ufpa.gustauber.databinding.RequisicaoLayoutBinding;

public class Requisicoes extends AppCompatActivity {

    private ActivityRequisicoesBinding vbinding;
    private RecyclerView rv;
    private ReqAdapter adapter;

    private List<ReqModel> listReq = new ArrayList<>();
    private AlertDialog dialogReq;

    private FirebaseAuth auth;
    private DatabaseReference refRequisicoes = FirebaseDatabase.getInstance().getReference("requisicoes");
    private String id_usuario;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vbinding = ActivityRequisicoesBinding.inflate(getLayoutInflater());

        setContentView(vbinding.getRoot());


        // remover modo escuro
        AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_NO);

        auth = FirebaseAuth.getInstance();
        rv = vbinding.recyclerReq;
        Objects.requireNonNull(getSupportActionBar()).setTitle("Área Motorista");

        id_usuario = Base64.getEncoder().encodeToString(Objects.requireNonNull(Objects.requireNonNull(auth.getCurrentUser()).getEmail()).getBytes());

        rv.setLayoutManager(new LinearLayoutManager(getApplicationContext(), RecyclerView.VERTICAL, false));
        rv.addItemDecoration(new DividerItemDecoration(getApplicationContext(), DividerItemDecoration.VERTICAL));
        rv.setHasFixedSize(true);
        adapter = new ReqAdapter(listReq, getApplicationContext(), id_usuario, this);
        rv.setAdapter(adapter);


        refRequisicoes.child(id_usuario).addValueEventListener(new ValueEventListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listReq.clear();
                for (DataSnapshot dado : snapshot.getChildren()){
                    ReqModel req = dado.getValue(ReqModel.class);
                    if ( req.getStatus() == 3){
                        AlertDialog.Builder b = new AlertDialog.Builder(Requisicoes.this);
                        ChamadaLayoutBinding bindingChamada = ChamadaLayoutBinding.inflate(getLayoutInflater());

                        bindingChamada.btnAceitarCorrida.setOnClickListener( aceitarView -> {
                            req.setStatus(1);
                            refRequisicoes.child(id_usuario).child(req.getId()).setValue(req).addOnCompleteListener(task -> {
                                String msg = "";
                                if ( task.isSuccessful()){
                                    msg = "Corrida aceita com sucesso!";
                                }else{
                                    msg = "Erro ao aceitar corridoa.";
                                }
                                dialogReq.dismiss();
                                Toast.makeText(Requisicoes.this, msg, Toast.LENGTH_SHORT).show();
                            });
                        });

                        bindingChamada.btnRecusarCorrida.setOnClickListener( recusarView -> {
                            req.setStatus(0);
                            refRequisicoes.child(id_usuario).child(req.getId()).setValue(req).addOnCompleteListener(task -> {
                                String msg = "";
                                if ( task.isSuccessful()){
                                    msg = "Corrida recusada com sucesso!";
                                }else{
                                    msg = "Erro ao recusar corridoa.";
                                }
                                dialogReq.dismiss();
                                Toast.makeText(Requisicoes.this, msg, Toast.LENGTH_SHORT).show();
                            });
                        });

                        bindingChamada.textPerguntaReq.setText(
                                String.format("Aceitas a corrida de %s ?", req.getNomeCliente())
                        );

                        bindingChamada.textDistancia.setText(
                                String.format("A distância é de %.2f metros até o cliente.", req.getDistancia())
                        );
                        b.setView(bindingChamada.getRoot());
                        b.setCancelable(false);
                        dialogReq = b.create();
                        dialogReq.show();
                    }
                    listReq.add(req);
                }

                if ( listReq.size() > 0){
                    vbinding.textSemReq.setVisibility(View.GONE);
                }

                adapter.notifyDataSetChanged();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_default, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.deslogar){
            auth.signOut();
            finish();
            startActivity(new Intent(getApplicationContext(), TelaPrincipal.class));
        }
        return super.onOptionsItemSelected(item);
    }

}