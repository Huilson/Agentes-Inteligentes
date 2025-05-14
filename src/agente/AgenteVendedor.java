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
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AgenteVendedor extends Agent {
    List<Carro> carros = new ArrayList<>();

    @Override
    protected void setup() {
        /**
         * Para um melhor entendimento desse algoritmo veja a classe AgenteComprador antes
         * */

        // Para gerar um carro aleatório use -> new Random().nextInt(5)
        // ou insira um valor de 0 a 4 para gerar um carro específico
        adicionarCarros(1);//adiciona um carro ao vendedor

        /** Registro do serviço do vendedor de carros nas páginas amarelas.
         * Para melhor entendimento veja a figura 4.5 do livro
         * algo que foi esquecido de comentar, DF significa Directory Facilitator.
         * DF é um agente, então é possível interagir com ele como qualquer outro
         * agente via mensagens ACL usando linguagem e modelos adequados.
         * As páginas amarelas é o que permite que um agente publique seu(s) serviço(s),
         * para que outros agentes possam facilmente descobrir e explorar tais serviços.
         * Um agente pode tanto publicar com procurar por serviços.
         */
        DFAgentDescription dfd = new DFAgentDescription();
        dfd.setName(getAID());//Identificador do agente, usado para depois criar a conversa

        /**
         * Vale lembrar que podemos criar, buscar, editar e excluir serviços durante qualquer
         * momento, portanto que o agente ainda esteja "vivo", ou seja, quando chamou o takeDown()
         * já era.
         * */
        //Descrição do serviço
        ServiceDescription sd = new ServiceDescription();
        sd.setType("troca-carro");// O tipo do serviço, igual do Comprador veja linha 50
        sd.setName("negociar-carros");// O nome do serviço
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

    public void adicionarCarros(int index) {
        Carro onix = new Carro(
                "ONIX",
                "Chevrolet",
                2025,
                3,
                List.of(Adicionais.AR_CONDICIONADO, Adicionais.CAMARA_RE, Adicionais.DIRECAO_HIDRAULICA),
                10,
                gerarValor(90000.00, 95000.00)
        );
        Carro cruze = new Carro(
                "CRUZE",
                "Chevrolet",
                2020,
                2,
                List.of(Adicionais.AR_CONDICIONADO, Adicionais.CAMARA_RE, Adicionais.DIRECAO_HIDRAULICA,
                        Adicionais.CENTRAL_MULTIMIDIA, Adicionais.VIDROS_ELETRICOS),
                8,
                gerarValor( 80000.00, 95000.00)
        );
        Carro gol = new Carro(
                "GOL",
                "Volkswagen",
                2010,
                1,
                List.of(Adicionais.BANCOS_DE_COURO),
                3,
                gerarValor(10000.00, 15000.00)
        );
        Carro pollo = new Carro(
                "POLLO",
                "Volkswagen",
                2025,
                3,
                List.of(Adicionais.CAMARA_RE, Adicionais.AR_CONDICIONADO, Adicionais.CENTRAL_MULTIMIDIA,
                        Adicionais.SISTEMA_DE_COLISAO, Adicionais.VIDROS_ELETRICOS),
                3,
                gerarValor(75000.00, 89290.00)
        );
        Carro mustangGT = new Carro(
                "MUSTANG GT",
                "Ford",
                2025,
                3,
                List.of(Adicionais.CAMARA_RE, Adicionais.AR_CONDICIONADO, Adicionais.CENTRAL_MULTIMIDIA,
                        Adicionais.SISTEMA_DE_COLISAO, Adicionais.VIDROS_ELETRICOS),
                3,
                gerarValor(45000.00, 549000.00)
        );
        switch (index) {
            case 0:
                System.out.println("O vendedor tem um Onix disponível");
                carros.add(onix);
                break;
            case 1:
                System.out.println("O vendedor tem um Cruze disponível, preço: "+cruze.getPreco()+"!");
                carros.add(cruze);
                break;
            case 2:
                System.out.println("O vendedor tem um Gol disponível");
                carros.add(gol);
                break;
            case 3:
                System.out.println("O vendedor tem um Pollo disponível");
                carros.add(pollo);
                break;
            case 4:
                System.out.println("O vendedor tem um Mustang GT disponível");
                carros.add(mustangGT);
                break;
        }
    }

    private BigDecimal gerarValor(double precoMinino, double precoMaximo){
        BigDecimal preco =  new BigDecimal(
                new Random().nextDouble(precoMaximo - precoMinino) + precoMinino
        );
        return preco.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal removerCarro(String carroVendido) {
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
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);// Call for Proposal, da para dizer
            //que isso funciona quase como um anúncio no mercado, mas aqui ele fica aguardando alguém chamar
            ACLMessage msg = myAgent.receive(mt);// Resposta da mensagem, caso um comprador chame
            if (msg != null) {
                // Um(s) comprador recebeu a call
                String prospostaComprador = msg.getContent();// Conteúdo da Mensagem, é o que o comprador deseja comprar
                System.out.println("Resposta de um comprador recebida, ele deseja um: " + prospostaComprador);
                ACLMessage resposta = msg.createReply();// Resposta da mensagem, se tem ou não o que o comprador quer, vai ser escrito aqui

                // Primeiro precisa ver se tem o carro que o comprador quer
                Carro carro = buscarCarro(prospostaComprador);

                if (carro != null) {
                    // O carro requerido está disponível para venda
                    resposta.setPerformative(ACLMessage.PROPOSE);
                    resposta.setContent(carro.getPreco().toString());//retorna só o preço do carro
                } else {
                    // O carro requerido NÃO está disponível para venda
                    resposta.setPerformative(ACLMessage.REFUSE);
                    resposta.setContent("not-available");
                }
                myAgent.send(resposta);//envia a resposta ao(s) compradore(s)
            } else {
                block();
            }
        }
    }  // End of inner class Ofertar Carro

    private class VenderCarro extends CyclicBehaviour {
        public void action() {
            //mesma lógica da oferta
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Aqui o comprador aceitou a proposta
                String prospostaComprador = msg.getContent();
                ACLMessage reply = msg.createReply();

                /** Durante a troca de mensagens pode acontecer do vendedor ter vendido o carro para
                 * outro comprador, por isso antes de INFORMAR o comprador que o carro será dele é
                 * preciso buscar o carro novamente, se ele existir remove o carro INFORMA que a
                 * compra deu certo, mas e o conteúdo da mensagem? Daí tem que ver, pode ser o carro
                 * em si ou qualquer outra coisa, nesse nosso código não precisa passar nada, afinial
                 * é só um faz de conta, mas se quiser dá para passar um conteúdo na mensagem.
                */
                BigDecimal preco = removerCarro(prospostaComprador);
                if (preco != null) {
                    reply.setPerformative(ACLMessage.INFORM);//Informar que carro foi vendido
                    System.out.println(prospostaComprador + " vendido para " + msg.getSender().getName());
                } else {
                    // O carro foi vendido para outro comprador enquanto o tramite ocorria.
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