package benicio.ufpa.gustauber.activity;

import static java.lang.Math.atan2;
import static java.lang.Math.cos;
import static java.lang.Math.sin;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

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
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.ItemizedIconOverlay;
import org.osmdroid.views.overlay.ItemizedOverlayWithFocus;
import org.osmdroid.views.overlay.OverlayItem;
import org.osmdroid.views.overlay.compass.CompassOverlay;
import org.osmdroid.views.overlay.compass.InternalCompassOrientationProvider;
import org.osmdroid.views.overlay.gestures.RotationGestureOverlay;


import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Objects;

import android.Manifest;

import com.github.clans.fab.FloatingActionButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import benicio.ufpa.gustauber.EsperaActivity;
import benicio.ufpa.gustauber.Model.ReqModel;
import benicio.ufpa.gustauber.Model.UserModel;
import benicio.ufpa.gustauber.R;
import benicio.ufpa.gustauber.databinding.CarregandoLayoutBinding;

public class ClienteActivity extends AppCompatActivity {

    private static final int RAIO_TERRA = 6371;

    private MapView map = null;
    double latitude, longitude;
    private FloatingActionButton fab_onde_estou, fab_solicitar;

    private Boolean carregando = false;
    private Boolean pointerAlreadyCreated = false;

    private FirebaseAuth auth;
    private DatabaseReference refRequisicoes = FirebaseDatabase.getInstance().getReference("requisicoes");
    private DatabaseReference refUsuarios = FirebaseDatabase.getInstance().getReference("users");

    private List<UserModel> listMotoristas = new ArrayList<>();

    private Dialog dialogCarregando;
    String idAtualUser;

    double menorDistancia = -1.0;
    UserModel motoristaMaisPerto = new UserModel();
    UserModel usuarioLogado = new UserModel();
    String idMotoristaPerto;

    private String TAG = "motorista";
    private Boolean corridaEmAndamento = false;


    @SuppressLint("MissingInflatedId")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Context ctx = getApplicationContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        setContentView(R.layout.activity_cliente);

        auth = FirebaseAuth.getInstance();
        idAtualUser = Base64.getEncoder().encodeToString(Objects.requireNonNull(Objects.requireNonNull(auth.getCurrentUser()).getEmail()).getBytes());
        criarDialogCarregando();

        // pegar todos os motoristas cadastrados no app
        refUsuarios.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                listMotoristas.clear();
                for ( DataSnapshot dado : snapshot.getChildren()){
                    UserModel motorista = dado.getValue(UserModel.class);
                    if ( motorista.getMotorista() ){
                        listMotoristas.add(motorista);
                    }
                }

                // pega o motorista com a menor distancia
                for (UserModel moto : listMotoristas) {
                    if (menorDistancia == -1.0){
                        menorDistancia = calcularDistancia(
                                moto.getLat(),
                                moto.getLongi(),
                                latitude,
                                longitude
                        );
                        motoristaMaisPerto.setEmail(moto.getEmail());
                        motoristaMaisPerto.setNome(moto.getNome());
                    }else{
                        Double distanciaAtual = calcularDistancia(
                                moto.getLat(),
                                moto.getLongi(),
                                latitude,
                                longitude
                        );
                        if (distanciaAtual < menorDistancia){
                            menorDistancia = distanciaAtual;
                            motoristaMaisPerto.setEmail(moto.getEmail());
                            motoristaMaisPerto.setNome(moto.getNome());
                        }
                    }
                }

                idMotoristaPerto = Base64.getEncoder().encodeToString(motoristaMaisPerto.getEmail().getBytes());

                // verificar se tem requisições aceitas .child(idAtualUser)
                refRequisicoes.child(idMotoristaPerto).addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for (DataSnapshot dado : snapshot.getChildren()){

                            ReqModel req = dado.getValue(ReqModel.class);
                            if (req.getStatus() == 1 || req.getStatus() == 3){
                                Toast.makeText(ctx, "Você tem uma corrida em ativa!", Toast.LENGTH_SHORT).show();
                                corridaEmAndamento = true;
                                Intent i = new Intent(getApplicationContext(), EsperaActivity.class);
                                i.putExtra("idMotorista", idMotoristaPerto);
                                i.putExtra("idUser", idAtualUser);
                                startActivity(i);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }


            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        // pegar dados do usuario logado
        refUsuarios.child(idAtualUser).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                UserModel userReq = snapshot.getValue(UserModel.class);

                usuarioLogado.setLat(userReq.getLat());
                usuarioLogado.setLongi(userReq.getLongi());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

        map = findViewById(R.id.map);
        fab_onde_estou = findViewById(R.id.fab_onde_estou);
        fab_solicitar = findViewById(R.id.fab_solicitar);

        // gerar requisicao
        fab_solicitar.setOnClickListener( viewSolicit -> {

            // verificar status da requisição
            refRequisicoes
                    .child(idMotoristaPerto)
                    .child(idAtualUser).addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            ReqModel model = snapshot.getValue(ReqModel.class);
                            if ( model.getStatus() == 0 || model.getStatus() == 2){
                                corridaEmAndamento = false;
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {

                        }
                    });

            if ( !corridaEmAndamento) {
                if (!carregando) {
                    if (listMotoristas.size() > 0) {

                        dialogCarregando.show();

                        // Gerar a solicitacao em si
                        @SuppressLint("DefaultLocale") ReqModel reqModel = new ReqModel(
                                motoristaMaisPerto.getNome(),
                                Objects.requireNonNull(auth.getCurrentUser()).getDisplayName(),
                                String.format("%,.2f", ((menorDistancia / 10000) * 2)),
                                idAtualUser,
                                3,
                                (menorDistancia / 10000),
                                usuarioLogado.getLat(),
                                usuarioLogado.getLongi()
                        );

                        refRequisicoes
                                .child(idMotoristaPerto)
                                .child(idAtualUser).setValue(reqModel).addOnCompleteListener(task -> {
                                    String msg = "";
                                    if (task.isSuccessful()) {
                                        msg = "Requisição criada, aguarde confirmação";
                                        corridaEmAndamento = true;
                                    } else {
                                        msg = "Erro ao fazer requisiçaõ!";
                                    }
                                    Toast.makeText(ctx, msg, Toast.LENGTH_SHORT).show();
                                    dialogCarregando.dismiss();
                                });

                    } else {
                        Toast.makeText(ctx, "Sem motorista disponível.", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(ctx, "Espere um pouco!", Toast.LENGTH_SHORT).show();
                }
            }else{
                Intent i = new Intent(getApplicationContext(), EsperaActivity.class);
                i.putExtra("idMotorista", idMotoristaPerto);
                i.putExtra("idUser", idAtualUser);
                startActivity(i);
                Toast.makeText(ctx, "Você ainda tem uma corrida ativa.", Toast.LENGTH_SHORT).show();
            }
        });


        map.setTileSource(TileSourceFactory.MAPNIK);

        map.setBuiltInZoomControls(true);
        map.setMultiTouchControls(true);

        // add compass
        CompassOverlay compass = new CompassOverlay(this, new InternalCompassOrientationProvider(this), map);
        compass.enableCompass();
        map.getOverlays().add(compass);

        // add rotation
        RotationGestureOverlay mRotationGestureOverlay = new RotationGestureOverlay(this, map);
        mRotationGestureOverlay.setEnabled(true);
        map.getOverlays().add(mRotationGestureOverlay);

        getLocation();

        fab_onde_estou.setOnClickListener( btn ->{
            if (!carregando){
                getLocation();
            }
        });



    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.deslogar){
            finish();
            auth.signOut();
            startActivity(new Intent(getApplicationContext(), TelaPrincipal.class));
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_default, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public void onResume() {
        super.onResume();
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    private void createPointer(){
        if (!pointerAlreadyCreated) {
            pointerAlreadyCreated = true;
            //your items
            ArrayList<OverlayItem> items = new ArrayList<OverlayItem>();
            items.add(new OverlayItem("Você", "Você esta aqui!", new GeoPoint(latitude, longitude))); // Lat/Lon decimal degrees

            //the overlay
            ItemizedOverlayWithFocus<OverlayItem> mOverlay = new ItemizedOverlayWithFocus<OverlayItem>(items,
                    new ItemizedIconOverlay.OnItemGestureListener<OverlayItem>() {
                        @Override
                        public boolean onItemSingleTapUp(final int index, final OverlayItem item) {
                            return true;
                        }

                        @Override
                        public boolean onItemLongPress(final int index, final OverlayItem item) {
                            return false;
                        }
                    }, this);

            mOverlay.setFocusItemsOnTap(true);

            map.getOverlays().add(mOverlay);
        }
    }

    private void getLocation() {
        dialogCarregando.show();
        carregando = true;
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        LocationListener locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                 latitude = location.getLatitude();
                 longitude = location.getLongitude();

                 createPointer();

                IMapController mapController = map.getController();
                mapController.setZoom(20.0);
                GeoPoint startPoint = new GeoPoint(latitude, longitude);
                mapController.setCenter(startPoint);

                Toast.makeText(ClienteActivity.this,
                        String.format("lat %s \n long %s", latitude, longitude), Toast.LENGTH_SHORT).show();
                carregando = false;
                dialogCarregando.dismiss();
                locationManager.removeUpdates(this);


            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {}

            @Override
            public void onProviderEnabled(String provider) {}

            @Override
            public void onProviderDisabled(String provider) {}
        };

        // Solicite atualizações de localização usando o provedor de rede
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
        } else {
            // Permissão de localização não concedida, trate esse caso adequadamente
        }

    }

    public static double calcularDistancia(double lat1, double lon1, double lat2, double lon2) {
        double distanciaLat = toRadians(lat2 - lat1);
        double distanciaLon = toRadians(lon2 - lon1);

        double a = sin(distanciaLat / 2) * sin(distanciaLat / 2)
                + cos(toRadians(lat1)) * cos(toRadians(lat2))
                * sin(distanciaLon / 2) * sin(distanciaLon / 2);

        double c = 2 * atan2(sqrt(a), sqrt(1 - a));

        return RAIO_TERRA * c;
    }

    public void criarDialogCarregando(){
        if (!isFinishing()) {
            AlertDialog.Builder b = new AlertDialog.Builder(ClienteActivity.this);
            b.setCancelable(false);
            b.setView(CarregandoLayoutBinding.inflate(getLayoutInflater()).getRoot());
            dialogCarregando = b.create();
        }
    }

}