package benicio.ufpa.gustauber.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthWeakPasswordException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import org.osmdroid.api.IMapController;
import org.osmdroid.util.GeoPoint;

import java.util.Base64;
import java.util.Objects;

import benicio.ufpa.gustauber.Model.UserModel;
import benicio.ufpa.gustauber.databinding.ActivityTelaPrincipalBinding;
import benicio.ufpa.gustauber.databinding.CarregandoLayoutBinding;
import benicio.ufpa.gustauber.databinding.LoginAndRegisterLayoutBinding;

public class TelaPrincipal extends AppCompatActivity {

    private ActivityTelaPrincipalBinding vbinding;
    private Dialog dialog_template;
    private FirebaseAuth auth;
    private DatabaseReference usuariosRef;

    private static final int REQUEST_CODE_PERMISSIONS = 1;
    private static final String[] REQUIRED_PERMISSIONS = {
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    };

    private Dialog dialogCarregando;
    private Double latitude, longitude;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        vbinding = ActivityTelaPrincipalBinding.inflate(getLayoutInflater());
        setContentView(vbinding.getRoot());
        FirebaseApp.initializeApp(this);
        usuariosRef = FirebaseDatabase.getInstance().getReference("users");
        auth = FirebaseAuth.getInstance();

        AppCompatDelegate.setDefaultNightMode( AppCompatDelegate.MODE_NIGHT_NO);
        Objects.requireNonNull(getSupportActionBar()).hide();

        criarDialogCarregando();

        vbinding.btnLogin.setOnClickListener( viewLogin -> {
            AlertDialog.Builder b = new AlertDialog.Builder(TelaPrincipal.this);
            LoginAndRegisterLayoutBinding bindingLogin = LoginAndRegisterLayoutBinding.inflate(getLayoutInflater());
            bindingLogin.textDinamicRegLog.setText("Faça Login!");
            bindingLogin.btnSubmeter.setOnClickListener( viewSub ->{
                String email,senha;
                email = bindingLogin.editEmail.getText().toString();
                senha = bindingLogin.editSenha.getText().toString();

                if (!email.isEmpty()){
                    if ( !senha.isEmpty()){
                        logarConta(email, senha);
                    }else{
                        Toast.makeText(this, "Preencha o campo senha!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(this, "Preencha o campo email!", Toast.LENGTH_SHORT).show();
                }
            });
            bindingLogin.btnCancelar.setOnClickListener(viewCancel ->{
                dialog_template.dismiss();
            });

            bindingLogin.editConfirmarSenha.setVisibility(View.GONE);
            bindingLogin.textConfirmSenha.setVisibility(View.GONE);
            bindingLogin.switchMotorista.setVisibility(View.GONE);
            bindingLogin.textNome.setVisibility(View.GONE);
            bindingLogin.editNome.setVisibility(View.GONE);

            b.setCancelable(false);
            b.setView(bindingLogin.getRoot());
            dialog_template = b.create();
            dialog_template.show();
        });


        vbinding.btnRegister.setOnClickListener( viewRegister -> {
            AlertDialog.Builder b = new AlertDialog.Builder(TelaPrincipal.this);
            LoginAndRegisterLayoutBinding bindingLogin = LoginAndRegisterLayoutBinding.inflate(getLayoutInflater());
            bindingLogin.textDinamicRegLog.setText("Faça o seu Registro!");
            bindingLogin.btnSubmeter.setOnClickListener( viewSub ->{
            String nome,email,senha,confSenha;
            Boolean motorista;

            nome = bindingLogin.editNome.getText().toString();
            email = bindingLogin.editEmail.getText().toString();
            senha = bindingLogin.editSenha.getText().toString();
            confSenha = bindingLogin.editConfirmarSenha.getText().toString();
            motorista = bindingLogin.switchMotorista.isChecked();

            if (!nome.isEmpty()){
                if(!email.isEmpty()){
                    if (!senha.isEmpty()){
                        if(!confSenha.isEmpty()){
                            if (senha.equals(confSenha)){
                                // cadastrar no database
                                cadastarNoDatabse(email,senha, nome, motorista);
                            }else{
                                Toast.makeText(this, "As senhas precisam ser iguais!", Toast.LENGTH_SHORT).show();
                            }
                        }else{
                            Toast.makeText(this, "Preencha o campo confirmar senha!", Toast.LENGTH_SHORT).show();
                        }
                    }else{
                        Toast.makeText(this, "Preencha o campo senha!", Toast.LENGTH_SHORT).show();
                    }
                }else{
                    Toast.makeText(this, "Preencha o campo email!", Toast.LENGTH_SHORT).show();
                }
            }else{
                Toast.makeText(this, "Preencha o campo nome!", Toast.LENGTH_SHORT).show();
            }

            });
            bindingLogin.btnCancelar.setOnClickListener(viewCancel ->{
                dialog_template.dismiss();
            });
            b.setCancelable(false);
            b.setView(bindingLogin.getRoot());
            dialog_template = b.create();
            dialog_template.show();
        });

    getLocation();
    permissoes();

    }

    public  void permissoes(){
        if (!arePermissionsGranted()) {
            // Solicite as permissões
            ActivityCompat.requestPermissions(this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS);
        } else {
            // As permissões já foram concedidas, execute a ação necessária
        }
    }

    // Método para verificar se todas as permissões necessárias foram concedidas
    private boolean arePermissionsGranted() {
        for (String permission : REQUIRED_PERMISSIONS) {
            if (ContextCompat.checkSelfPermission(this, permission)
                    != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    // Método chamado após o usuário responder à solicitação de permissões
    @SuppressLint("MissingSuperCall")
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (areAllPermissionsGranted(grantResults)) {
                // Todas as permissões foram concedidas, execute a ação necessária
                // ...
            } else {
                // Alguma ou todas as permissões foram negadas, lide com isso adequadamente
                // ...
            }
        }
    }

    // Método para verificar se todas as permissões foram concedidas
    private boolean areAllPermissionsGranted(int[] grantResults) {
        for (int result : grantResults) {
            if (result != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        dialogCarregando.show();

        if (auth.getCurrentUser() != null){
            usuariosRef.child(Base64.getEncoder().encodeToString(auth.getCurrentUser().getEmail().getBytes())).addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    UserModel user = snapshot.getValue(UserModel.class);

                    assert user != null;
                    Toast.makeText(TelaPrincipal.this,
                            String.format("Bem-vindo de volta %s!", user.getNome()),
                            Toast.LENGTH_SHORT).show();
                    if (user.getMotorista()){
                        startActivity(new Intent(getApplicationContext(), Requisicoes.class));
                    }else{
                        startActivity(new Intent(getApplicationContext(), ClienteActivity.class));
                    }
                    finish();
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }else{
            dialogCarregando.dismiss();
        }

    }

    private void cadastarNoDatabse(String email, String senha, String nome, Boolean motorista){

            String id = Base64.getEncoder().encodeToString(email.getBytes());

            usuariosRef.child(id).setValue(new UserModel( nome, email, motorista,latitude, longitude)).addOnCompleteListener(task -> {
                if (task.isSuccessful()){
                    if (latitude != null)
                        criarConta(email, senha, nome, motorista);
                    else
                        Toast.makeText(this, "Espere a sua localização carregar", Toast.LENGTH_SHORT).show();
                }else{
                    Toast.makeText(TelaPrincipal.this, "Erro ao criar conta, teve novamente!", Toast.LENGTH_LONG).show();
                }
            });
    }
    private void criarConta(String email, String senha, String nome, Boolean motorista){
        auth.createUserWithEmailAndPassword(email, senha).addOnCompleteListener(task -> {
            if (task.isSuccessful() ){
                FirebaseUser user = auth.getCurrentUser();

                UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                        .setDisplayName(nome)
                        .build();

                user.updateProfile(profileUpdates)
                        .addOnCompleteListener(updateTask -> {
                            if (updateTask.isSuccessful()) {
                                Toast.makeText(getApplicationContext(), "Conta criada com sucesso!",
                                        Toast.LENGTH_LONG).show();
                                if (motorista){
                                    startActivity(new Intent(getApplicationContext(), Requisicoes.class));
                                }else{
                                    startActivity(new Intent(getApplicationContext(), ClienteActivity.class));
                                }
                            } else {
                                Toast.makeText(getApplicationContext(), "Erro ao atualizar o perfil do usuário.",
                                        Toast.LENGTH_LONG).show();
                            }
                        });
            }else{
                try {
                    throw Objects.requireNonNull(task.getException());
                } catch(FirebaseAuthWeakPasswordException e) {
                    Toast.makeText(getApplicationContext(), "Senha muito fraca!",
                            Toast.LENGTH_LONG).show();
                } catch(FirebaseAuthInvalidCredentialsException e) {
                    Toast.makeText(getApplicationContext(), "E-mail inválido!",
                            Toast.LENGTH_LONG).show();
                } catch(FirebaseAuthUserCollisionException e) {
                    Toast.makeText(getApplicationContext(), "E-mail já cadastrado!",
                            Toast.LENGTH_LONG).show();
                } catch(Exception e) {
                    Toast.makeText(getApplicationContext(), "Erro!",
                            Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void logarConta(String email, String senha){
        auth.signInWithEmailAndPassword(email, senha).addOnCompleteListener(task -> {
            if ( task.isSuccessful() ){
                usuariosRef.child(Base64.getEncoder().encodeToString(email.getBytes())).addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        UserModel user = snapshot.getValue(UserModel.class);

                        assert user != null;
                        Toast.makeText(TelaPrincipal.this,
                                String.format("Bem-vindo de volta %s!", user.getNome()),
                                Toast.LENGTH_SHORT).show();
                        if (user.getMotorista()){
                            startActivity(new Intent(getApplicationContext(), Requisicoes.class));
                        }else{
                            startActivity(new Intent(getApplicationContext(), ClienteActivity.class));
                        }
                        finish();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });

            }else{
                try {
                    throw Objects.requireNonNull(task.getException());
                }
                catch (FirebaseAuthInvalidUserException ee){
                    Toast.makeText(this, "Conta não encontrada!", Toast.LENGTH_SHORT).show();
                }
                catch (FirebaseAuthInvalidCredentialsException e){
                    Toast.makeText(this, "E-mail ou senha errado!", Toast.LENGTH_SHORT).show();
                }catch (Exception eGeneric){
                    Toast.makeText(this, "Erro ao fazer login", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void getLocation() {

        // Abrir dialogo carregando

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                // setando variaveis global
                latitude = location.getLatitude();
                longitude = location.getLongitude();

            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Solicite atualizações de localização usando o provedor de rede
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            // Permissão de localização não concedida, trate esse caso adequadamente
            permissoes();
        }

    }

    public void criarDialogCarregando(){
        if (!isFinishing()) {
            AlertDialog.Builder b = new AlertDialog.Builder(TelaPrincipal.this);
            b.setCancelable(false);
            b.setView(CarregandoLayoutBinding.inflate(getLayoutInflater()).getRoot());
            dialogCarregando = b.create();
        }
    }
}