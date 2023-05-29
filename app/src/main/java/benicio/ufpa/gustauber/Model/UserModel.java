package benicio.ufpa.gustauber.Model;

public class UserModel {
    String nome,email;
    Boolean motorista;
    Double lat, longi;

    public UserModel() {
    }


    public UserModel(String nome, String email, Boolean motorista, Double lat, Double longi) {
        this.nome = nome;
        this.email = email;
        this.motorista = motorista;
        this.lat = lat;
        this.longi = longi;
    }

    public Double getLat() {
        return lat;
    }

    public void setLat(Double lat) {
        this.lat = lat;
    }

    public Double getLongi() {
        return longi;
    }

    public void setLongi(Double longi) {
        this.longi = longi;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Boolean getMotorista() {
        return motorista;
    }

    public void setMotorista(Boolean motorista) {
        this.motorista = motorista;
    }
}
