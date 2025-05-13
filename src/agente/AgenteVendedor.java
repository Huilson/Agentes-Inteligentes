package agente;

import jade.core.Agent;
import jade.core.behaviours.CyclicBehaviour;
import jade.core.behaviours.OneShotBehaviour;
import jade.domain.DFService;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;
import jade.domain.FIPAException;

import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import modelo.Adicionais;
import modelo.Carro;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AgenteVendedor extends Agent {
    List<Carro> carros = new ArrayList<>();

    @Override
    protected void setup() {
        adicionarCarros();//adiciona um carro aleatório ao vendedor

        //Registro do serviço do vendedor de carros nas páginas amarelas
        //Directory Facilitator
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());//O nome do serviço

        //Descrição do serviço
        ServiceDescription sd = new ServiceDescription();
        sd.setType("troca-carro");//O tipo do serviço
        sd.setName("negociar-carro");//O nome do serviço
        dfd.addServices(sd);
        try {
            DFService.register(this, dfd);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }

        System.out.println("Hello World. I’m an agent!");
        System.out.println("My local-name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are:");
        Iterator it = getAID().getAllAddresses();
        while (it.hasNext()) {
            System.out.println("- " + it.next());
        }

        /**
         * COMPORTAMENTOS DO VENDEDOR
         * */
        addBehaviour(new OfertarCarro());
        addBehaviour(new VenderCarro());
    }

    /**
     * COMPORTAMENTOS DO AGENTE (BEHAVIOUR)
     */
    private Carro buscarCarro(String carroRequisitado) {
        for (Carro carro : carros) {//Itera pelo carros da lista
            if (carro.getModelo().equals(carroRequisitado)) {//por enquanto só busca pelo modelo
                System.out.println("Carro encontrado, podemos negociar!");
                return carro;//encontrou o carro
            }
        }//fim for
        return null;//carro não encontrado
    }

    public void adicionarCarros() {
        Carro onix = new Carro(
                "Onix",
                "Chevrolet",
                2025,
                3,
                List.of(Adicionais.AR_CONDICIONADO, Adicionais.CAMARA_RE, Adicionais.DIRECAO_HIDRAULICA),
                10,
                new Random().nextDouble(95000.00 - 90000.00) + 90000.00 //valor aleatório do preço do carro
        );//Formula para gerar preço (máximo - mínimo) + mínimo
        Carro cruze = new Carro(
                "Cruze",
                "Chevrolet",
                2020,
                2,
                List.of(Adicionais.AR_CONDICIONADO, Adicionais.CAMARA_RE, Adicionais.DIRECAO_HIDRAULICA,
                        Adicionais.CENTRAL_MULTIMIDIA, Adicionais.VIDROS_ELETRICOS),
                8,
                new Random().nextDouble(95000.00 - 80000.00) + 80000.00 //valor aleatório do preço do carro
        );//Formula para gerar preço (máximo - mínimo) + mínimo
        Carro gol = new Carro(
                "Gol",
                "Volkswagen",
                2010,
                1,
                List.of(Adicionais.BANCOS_DE_COURO),
                3,
                new Random().nextDouble(15000.00 - 10000.00) + 10000.00 //valor aleatório do preço do carro
        );//Formula para gerar preço (máximo - mínimo) + mínimo
        Carro pollo = new Carro(
                "Pollo",
                "Volkswagen",
                2025,
                3,
                List.of(Adicionais.CAMARA_RE, Adicionais.AR_CONDICIONADO, Adicionais.CENTRAL_MULTIMIDIA,
                        Adicionais.SISTEMA_DE_COLISAO, Adicionais.VIDROS_ELETRICOS),
                3,
                new Random().nextDouble(89290.00 - 75000.00) + 75000.00 //valor aleatório do preço do carro
        );//Formula para gerar preço (máximo - mínimo) + mínimo
        Carro mustangGT = new Carro(
                "Mustang GT",
                "Ford",
                2025,
                3,
                List.of(Adicionais.CAMARA_RE, Adicionais.AR_CONDICIONADO, Adicionais.CENTRAL_MULTIMIDIA,
                        Adicionais.SISTEMA_DE_COLISAO, Adicionais.VIDROS_ELETRICOS),
                3,
                new Random().nextDouble(549000.00 - 45000.00) + 45000.00 //valor aleatório do preço do carro
        );//Formula para gerar preço (máximo - mínimo) + mínimo
        switch (new Random().nextInt(5)) {
            case 0:
                System.out.println("O vendedor tem um Onix disponível");
                carros.add(onix);
                break;
            case 1:
                System.out.println("O vendedor tem um Onix disponível");
                carros.add(cruze);
                break;
            case 2:
                System.out.println("O vendedor tem um Onix disponível");
                carros.add(gol);
                break;
            case 3:
                System.out.println("O vendedor tem um Onix disponível");
                carros.add(pollo);
                break;
            case 4:
                System.out.println("O vendedor tem um Onix disponível");
                carros.add(mustangGT);
                break;
        }
    }

    private Double removerCarro(String carroVendido) {
        Carro carro = buscarCarro(carroVendido);//Chama novamente a Função de busca

        addBehaviour(new OneShotBehaviour() {//COMPORTAMENTO PARA REMOVER CARRO DA LISTA
            public void action() {
                if (carro != null) {
                    carros.remove(carro);//Remove o carro que foi vendido da lista
                }
            }
        });
        if (carro != null) {
            return carro.getPreco();//Retorna o preço do carro
        }
        return null;//se não achou o carro retorna nulo
    }

    protected void takeDown() {
        // Remover das páginas amarelas
        try {
            DFService.deregister(this);
        } catch (FIPAException fe) {
            fe.printStackTrace();
        }
        System.out.println("Agente vendedor " + getAID().getName() + " terminado.");
    }

    /**
     * INNER CLASS
     */
    public class OfertarCarro extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);//Call for Proposal, anuncia no mercado
            ACLMessage msg = myAgent.receive(mt);//envio/resposta da mensagem
            if (msg != null) {
                // CFP Message received. Process it
                String prospostaComprador = msg.getContent();//Conteudo da Mensagem, é o que o comprador deseja comprar
                System.out.println("Resposta de um comprador recebida: " + prospostaComprador);
                ACLMessage reply = msg.createReply();//Resposta da mensagem, se tem ou não o que o comprador quer, vai ser escrito aqui

                Carro carro = buscarCarro(prospostaComprador);/*Busca pra ver se o livro existe*/

                if (carro != null) {
                    // The requested book is available for sale Reply with the preco
                    //O carro requerido está disponível para venda
                    reply.setPerformative(ACLMessage.PROPOSE);
                    reply.setContent(carro.getPreco().toString());//retorna só o preço do carro
                } else {
                    //O carro requerido NÃO está disponível para venda
                    reply.setPerformative(ACLMessage.REFUSE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);//envia a resposta ao(s) compradore(s)
            } else {
                block();
            }
        }
    }  // End of inner class Ofertar Carro

    private class VenderCarro extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // ACCEPT_PROPOSAL Message received. Process it
                String prospostaComprador = msg.getContent();
                ACLMessage reply = msg.createReply();

                Double preco = removerCarro(prospostaComprador);
                if (preco != null) {
                    reply.setPerformative(ACLMessage.INFORM);//Informar que carro foi vendido
                    System.out.println(prospostaComprador + " sold to agent " + msg.getSender().getName());
                } else {
                    // The requested book has been sold to another buyer in the meanwhile .
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("not-available");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }  // End of inner class Vender Carro
}//FIM DA CLASSE MAIN
