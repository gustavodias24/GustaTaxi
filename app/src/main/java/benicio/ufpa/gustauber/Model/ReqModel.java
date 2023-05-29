package benicio.ufpa.gustauber.Model;

public class ReqModel {

    public ReqModel() {

    }

    public ReqModel(String nomeMotorista, String nomeCliente, String valor, String id, int status, double distancia, double latDest, double longDest) {
        this.nomeMotorista = nomeMotorista;
        this.nomeCliente = nomeCliente;
        this.valor = valor;
        this.id = id;
        this.status = status;
        this.distancia = distancia;
        this.latDest = latDest;
        this.longDest = longDest;
    }

    String nomeMotorista,nomeCliente, valor, id;
    int status;

    public String getNomeMotorista() {
        return nomeMotorista;
    }

    public void setNomeMotorista(String nomeMotorista) {
        this.nomeMotorista = nomeMotorista;
    }

    double distancia, latDest, longDest;

    public double getLatDest() {
        return latDest;
    }

    public void setLatDest(double latDest) {
        this.latDest = latDest;
    }

    public double getLongDest() {
        return longDest;
    }

    public void setLongDest(double longDest) {
        this.longDest = longDest;
    }

    public double getDistancia() {
        return distancia;
    }

    public void setDistancia(double distancia) {
        this.distancia = distancia;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNomeCliente() {
        return nomeCliente;
    }

    public void setNomeCliente(String nomeCliente) {
        this.nomeCliente = nomeCliente;
    }

    public String getValor() {
        return valor;
    }

    public void setValor(String valor) {
        this.valor = valor;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
