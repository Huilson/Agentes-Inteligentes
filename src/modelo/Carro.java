package modelo;

import java.math.BigDecimal;
import java.util.List;

public class Carro {
    /**
     * Modelo
     * Marca
     * Ano de Frabricação
     * Estado de Conservação
     * Km rodados
     * Adicionais (ENUM)
     * Preço Base
     * Nota
     * */
    private String modelo;
    private String marca;
    private int ano;
    private int estado;//1-velho; 2-usado; 3-novo;
    private List<Adicionais> adicionais;
    private int nota;//De 0 a 10; sendo 10 a melhor a nota
    private BigDecimal preco;

    //CONSTRUTORES
    public Carro() {
    }

    public Carro(String modelo, String marca, int ano, int estado, List<Adicionais> adicionais, int nota, BigDecimal preco) {
        this.modelo = modelo;
        this.marca = marca;
        this.ano = ano;
        this.estado = estado;//1-VELHO, 2-USADO, 3-NOVO
        this.adicionais = adicionais;
        this.nota = nota;//de 0 a 100, isso impacta no desconto do carro
        this.preco = preco;
    }

    //GETTERS E SETTERS

    public String getMarca() {
        return marca;
    }

    public void setMarca(String marca) {
        this.marca = marca;
    }

    public int getAno() {
        return ano;
    }

    public void setAno(int ano) {
        this.ano = ano;
    }

    public int getEstado() {
        return estado;
    }

    public void setEstado(int estado) {
        this.estado = estado;
    }

    public List<Adicionais> getAdicionais() {
        return adicionais;
    }

    public void setAdicionais(List<Adicionais> adicionais) {
        this.adicionais = adicionais;
    }

    public int getNota() {
        return nota;
    }

    public void setNota(int nota) {
        this.nota = nota;
    }

    public BigDecimal getPreco() {
        return preco;
    }

    public void setPreco(BigDecimal preco) {
        this.preco = preco;
    }

    public String getModelo() {
        return modelo;
    }

    public void setModelo(String modelo) {
        this.modelo = modelo;
    }
}

