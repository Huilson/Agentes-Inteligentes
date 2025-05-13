package agente;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.Behaviour;
import jade.core.behaviours.TickerBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import modelo.Carro;

import java.math.BigDecimal;
import java.util.Scanner;

public class AgenteComprador extends Agent {
    Carro carro = new Carro();
    Scanner info = new Scanner(System.in);
    boolean ok = true;
    //Lista de agentes de venda disponivéis
    private AID[] agentesVendor;

    @Override
    protected void setup() {
        System.out.println("Agente " + getLocalName() + " inicializado.");
        carro = menu();

        addBehaviour(new TickerBehaviour(this, 60000) {
            protected void onTick() {
                System.out.println("Requisitando compra... "+carro.getModelo());
                //Procura por vendedores
                DFAgentDescription template = new DFAgentDescription();//Páginas amarelas
                ServiceDescription servico = new ServiceDescription();//Serviço
                servico.setType("troca-carro");//procura pelo serviço do tipo "venda carro"
                template.addServices(servico);
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    System.out.println("Agentes de venda encontrados:");
                    agentesVendor = new AID[result.length];//tamanho do array de agentes encontrados
                    for (int i = 0; i < result.length; ++i) {
                        agentesVendor[i] = result[i].getName();
                        System.out.println(agentesVendor[i].getName());
                    }
                }
                catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Perform the request
                myAgent.addBehaviour(new NegociarCarro());
            }
        } );
    }

    protected void takeDown() {
        // Printout a dismissal message
        System.out.println("Buyer-agent "+getAID().getName()+" terminating.");
    }

    private class NegociarCarro extends Behaviour {
        private AID melhorVendedor; // The agent who provides the best offer
        private BigDecimal melhorPreco;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            switch (step) {
                case 0:
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agentesVendor.length; ++i) {
                        cfp.addReceiver(agentesVendor[i]);
                    }
                    cfp.setContent(carro.getModelo());
                    cfp.setConversationId("book-trade");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); // Unique value
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    break;
                case 1:
                    // Receive all proposals/refusals from seller agents
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Reply received
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // This is an offer
                            BigDecimal price = new BigDecimal (reply.getContent());
                            if (melhorVendedor == null || price.equals(melhorPreco)) {
                                // This is the best offer at present
                                melhorPreco = price;
                                melhorVendedor = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= agentesVendor.length) {
                            // We received all replies
                            step = 2;
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    // Send the purchase order to the seller that provided the best offer
                    ACLMessage pedido = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    pedido.addReceiver(melhorVendedor);
                    pedido.setContent(carro.getModelo());
                    pedido.setConversationId("negociar-carros");
                    pedido.setReplyWith("Pedido`: "+System.currentTimeMillis());
                    myAgent.send(pedido);
                    // Prepare the template to get the purchase order reply
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("negociar-carro"),
                            MessageTemplate.MatchInReplyTo(pedido.getReplyWith()));
                    step = 3;
                    break;
                case 3:
                    // Receive the purchase order reply
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Purchase order reply received
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Purchase successful. We can terminate
                            System.out.println(carro.getModelo()+" foi comprado do vendedor "+reply.getSender().getName());
                            System.out.println("Preço = "+ melhorPreco);
                            myAgent.doDelete();
                        }
                        else {
                            System.out.println("Houve uma falha na negocição parece que o carro já foi vendido");
                        }

                        step = 4;
                    }
                    else {
                        block();
                    }
                    break;
            }
        }

        public boolean done() {
            if (step == 2 && melhorVendedor == null) {
                System.out.println("Não foi possível encontrar o : "+carro.getModelo()+" para vender!");
            }
            return ((step == 2 && melhorVendedor == null) || step == 4);
        }
    }  // End of inner class RequestPerformer


    public Carro menu(){
        while (ok) {
            System.out.println("\n\n\n\n\n\n\n\n\n\n");//limpar console
            System.out.println("Qual marca de carro você deseja comprar?");
            carro.setMarca(info.nextLine().toUpperCase());
            System.out.println("Qual modelo dessa marca?");
            carro.setModelo(info.nextLine().toUpperCase());
            System.out.println("Qual o ano de fabricação?");
            String ano = info.nextLine();
            System.out.println("Quanto você pode pagar?");
            String preco = info.nextLine();

            if (ano.matches("^\\d+$") && preco.matches("^\\d+$")) {
                carro.setAno(Integer.parseInt(ano));
                carro.setPreco(new BigDecimal(preco));
            } else {
                System.out.println("Ano ou preço inválidos!");
                continue;
            }

            System.out.println("Então o que você procura é...");
            System.out.println("Marca: " + carro.getMarca());
            System.out.println("Modelo: " + carro.getModelo());
            System.out.println("Ano: " + carro.getAno());
            System.out.println("Preco: " + carro.getPreco() + ",00 reais");
            System.out.println("Digite 1 para prosseguir ou 2 para refazer a busca");
            ok = info.nextInt() >= 2;//Se for maior ou igual a 2 a variavel recebe FALSE
        }
        System.out.println("Contactando vendedores, aguarde...");
        return carro;
    }
}
