import jade.core.Agent;

public class MeuAgente extends Agent {
    @Override
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " inicializado.");
    }
}