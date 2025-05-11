package agente;

import
import modelo.Adicionais;
import modelo.Carro;

import java.math.BigDecimal;
import java.util.List; ...

public class AgenteComprador extends Agent {
    // Modelo do carro desejado
    Carro onix = new Carro(
            "Onix",
            "Chevrolet",
            2025,
            3,
            0.0,
            List.of(Adicionais.AR_CONDICIONADO, Adicionais.BANCOS_DE_COURO),
            10,
            new BigDecimal("90000.00")
    );


    // Lista dos vendedores de carro
    private AID[] sellerAgents;

    // Put agent initializations here
    protected void setup() {
        // Printout a welcome message
        System.out.println("Olá! Agente comprador "+getAID().getName()+" está pronto.");

        // Get the title of the book to buy as a start-up argument
        // Pega o modelo do carro como inicio
        Object[] args = getArguments();
        if (args != null && args.length > 0) { // aqui no caso eu passaria uma lista de carros? ou puxaria só o onix?
            targetBookTitle = (String) args[0];
            System.out.println("O modelo do carro é "+targetBookTitle);

            // Add a TickerBehaviour that schedules a request to seller agents every minute
            addBehaviour(new TickerBehaviour(this, 60000) {
                protected void onTick() {
                    System.out.println("Trying to buy "+targetBookTitle);
                    // Update the list of seller agents
                    DFAgentDescription template = new DFAgentDescription();
                    ServiceDescription sd = new ServiceDescription();
                    sd.setType("book-selling");
                    template.addServices(sd);
                    try {
                        DFAgentDescription[] result = DFService.search(myAgent, template);
                        System.out.println("Found the following seller agents:");
                        sellerAgents = new AID[result.length];
                        for (int i = 0; i < result.length; ++i) {
                            sellerAgents[i] = result[i].getName();
                            System.out.println(sellerAgents[i].getName());
                        }
                    }
                    catch (FIPAException fe) {
                        fe.printStackTrace();
                    }

                    // Perform the request
                    myAgent.addBehaviour(new RequestPerformer());
                }
            } );
        }
        else {
            // Make the agent terminate
            System.out.println("No target book title specified");
            doDelete();
        }
    }

    // Put agent clean-up operations here
    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
    }
}
// FIM DA CLASSE MAIN