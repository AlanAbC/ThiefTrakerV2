package com.claresti.tt.thieftrakerv2;

/**
 * Created by Rikym on 05/04/2017.
 */

public class objetoSucesos {

    private String IDREPORTE;
    private String LATITUD;
    private String LONGITUD;
    private String FECHA_SUCESO;
    private String FECHA_REGISTRO;
    private String ID_TIPO;

    public objetoSucesos(String IDREPORTE, String LATITUD, String LONGITUD, String FECHA_SUCESO, String FECHA_REGISTRO, String ID_TIPO) {
        this.IDREPORTE = IDREPORTE;
        this.LATITUD = LATITUD;
        this.LONGITUD = LONGITUD;
        this.FECHA_SUCESO = FECHA_SUCESO;
        this.FECHA_REGISTRO = FECHA_REGISTRO;
        this.ID_TIPO = ID_TIPO;
    }

    public String getIDREPORTE() {
        return IDREPORTE;
    }

    public void setIDREPORTE(String IDREPORTE) {
        this.IDREPORTE = IDREPORTE;
    }

    public String getLATITUD() {
        return LATITUD;
    }

    public void setLATITUD(String LATITUD) {
        this.LATITUD = LATITUD;
    }

    public String getLONGITUD() {
        return LONGITUD;
    }

    public void setLONGITUD(String LONGITUD) {
        this.LONGITUD = LONGITUD;
    }

    public String getFECHA_SUCESO() {
        return FECHA_SUCESO;
    }

    public void setFECHA_SUCESO(String FECHA_SUCESO) {
        this.FECHA_SUCESO = FECHA_SUCESO;
    }

    public String getFECHA_REGISTRO() {
        return FECHA_REGISTRO;
    }

    public void setFECHA_REGISTRO(String FECHA_REGISTRO) {
        this.FECHA_REGISTRO = FECHA_REGISTRO;
    }

    public String getID_TIPO() {
        return ID_TIPO;
    }

    public void setID_TIPO(String ID_TIPO) {
        this.ID_TIPO = ID_TIPO;
    }
}
