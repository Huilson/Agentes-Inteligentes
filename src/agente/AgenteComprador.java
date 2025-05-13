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
import java.util.Iterator;
import java.util.Scanner;

public class AgenteComprador extends Agent {
    Carro carro = new Carro();
    Scanner info = new Scanner(System.in);
    boolean ok = true;
    //Lista de agentes de venda disponivéis
    private AID[] agentesVendor;

    @Override
    protected void setup() {
        System.out.println("Hello World. I’m an agent!");
        System.out.println("My local-name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are:");
        Iterator it = getAID().getAllAddresses();
        while (it.hasNext()) {
            System.out.println("- " + it.next());
        }

        carro = menu();

        addBehaviour(new TickerBehaviour(this, 60000) {
            protected void onTick() {
                System.out.println("Requisitando compra... "+carro.getModelo());
                //Procura por vendedores
                /**
                 * Cada serviço deve incluir: tipo, nome e outros necessários para usar o serviço
                 * e uma coleção de propriedades de serviços-específicos em forma de key-value (hash/map/etc).
                 * Um agente que deseja buscar por serviços deve fornecer também um DF com um TEMPLATE descritivo.
                 * */
                DFAgentDescription template = new DFAgentDescription();//Descrição do Template
                ServiceDescription servico = new ServiceDescription();//Cria um serviço
                servico.setType("troca-carro");//Cria um tipo para o serviço (obrigatório)
                template.addServices(servico);
                try {/**
                     * Segundo o livro, se um agente deseja publicar um ou mais serviço é preciso ter o DF
                     * com a descrição que inclui o AID do agente, uma lista de serviços, e se quiser uma lista
                     * de linguagens e modelos que outros agentes podem usar para interagir.
                     */
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

                // Comportamento para fazer as requisições de compra
                myAgent.addBehaviour(new NegociarCarro());
            }
        } );
    }

    protected void takeDown() {
        System.out.println("O agente "+getAID().getName()+" terminou o serviço e agora será encerrado.");
    }

    private class NegociarCarro extends Behaviour {
        private AID melhorVendedor; // The agent who provides the best offer
        private BigDecimal melhorPreco;  // The best offered price
        private int repliesCnt = 0; // The counter of replies from seller agents
        private MessageTemplate mt; // The template to receive replies
        private int step = 0;

        public void action() {
            System.out.println("Passo "+step);
            switch (step) {
                case 0:
                    System.out.println("\nVamos negociar!");
                    // Send the cfp to all sellers
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agentesVendor.length; ++i) {
                        System.out.println("Enviando mensagem para "+agentesVendor[i]);
                        cfp.addReceiver(agentesVendor[i]);
                    }
                    cfp.setContent(carro.getModelo());
                    cfp.setConversationId("negociar-carros");
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); //Tempo em millisegundos para criar chave única
                    System.out.println("Call For Proposal, chave gerada: "+cfp.getReplyWith());
                    myAgent.send(cfp);
                    // Prepare the template to get proposals
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("negociar-carros"),
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
                    step = 1;
                    try {
                        Thread.sleep(5000);//dar um tempinho para ler o console
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case 1:
                    // Recebe todas as respostas dos vendedores (seja ACEITE ou RECUSA)
                    ACLMessage reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Resposta de um vendedor, se for uma PROPOSTA toca o baile
                        if (reply.getPerformative() == ACLMessage.PROPOSE) {
                            // Oferta gerada pelo vendedor
                            BigDecimal preco = new BigDecimal(reply.getContent());

                            //Em BigDecimal a comparação é um pouco diferente, você precisa
                            //usar o compareTo, para comparar PRECO com MELHORPRECO
                            //se essa comparação retornar -1, significa que PRECO é menor que MELHORPRECO
                            if (melhorVendedor == null || preco.compareTo(melhorPreco) < 0) {
                                // This is the best offer at present
                                System.out.println("\nUm carro com menor preço encontrado, valor: "+preco);
                                melhorPreco = preco;
                                melhorVendedor = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        if (repliesCnt >= agentesVendor.length) {
                            // Todas as respostas recebidas, próxima etapa
                            step = 2;
                            try {
                                Thread.sleep(5000);//dar um tempinho para ler o console
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    }
                    else {
                        block();
                    }
                    break;
                case 2:
                    //Fazer pedido de comprar para o melhor vendedor
                    System.out.println("\nVamos fazer a proposta ao vendedor "+melhorVendedor+"!");
                    ACLMessage mensagemCompra = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);//Manda uma mensagem que foi aceita a proposta
                    mensagemCompra.addReceiver(melhorVendedor);
                    mensagemCompra.setContent(carro.getModelo());
                    mensagemCompra.setConversationId("negociar-carros");
                    mensagemCompra.setReplyWith("Pedido: "+System.currentTimeMillis());
                    myAgent.send(mensagemCompra);
                    // Prepara o template para fazer a resposta da ordem de compra
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("negociar-carros"),
                            MessageTemplate.MatchInReplyTo(mensagemCompra.getReplyWith()));
                    step = 3;
                    try {
                        Thread.sleep(5000);//dar um tempinho para ler o console
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    break;
                case 3:
                    // Resposta da mensagem do passo acima
                    System.out.println("\nVamos tentar fechar o acordo com "+melhorVendedor+"!");
                    reply = myAgent.receive(mt);
                    if (reply != null) {
                        // Se recebeu a mensagem imforma que deu tudo certo
                        if (reply.getPerformative() == ACLMessage.INFORM) {
                            // Compra bem sucedida
                            System.out.println(carro.getModelo()+" foi comprado do vendedor "+reply.getSender().getName());
                            System.out.println("Preço = "+ melhorPreco);
                            System.out.println("Parabéns pela sua nova conquista!");
                            myAgent.doDelete();//encerrando o agente
                        }
                        else {
                            System.out.println("Houve uma falha na negocição parece que o carro já foi vendido");
                        }

                        step = 4;
                        try {
                            Thread.sleep(5000);//dar um tempinho para ler o console
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
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