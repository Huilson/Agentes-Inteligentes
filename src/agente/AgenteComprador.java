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
    boolean ok = true;//Usado no REGEX pra converter String pra Numero (int e BigDecimal)
    //Lista de agentes de venda disponivéis
    private AID[] agentesVendor;

    @Override
    protected void setup() {
        /*System.out.println("Hello World. I’m an agent!");
        System.out.println("My local-name is " + getAID().getLocalName());
        System.out.println("My GUID is " + getAID().getName());
        System.out.println("My addresses are:");
        Iterator it = getAID().getAllAddresses();
        while (it.hasNext()) {
            System.out.println("- " + it.next());
        }*/

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
                //servico.setName("negociar-carros");
                servico.setType("troca-carro");//Cria um tipo para o serviço (obrigatório), igual do Vendedor veja linha 53
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
        private AID melhorVendedor; // Armazena o agente de venda com o melhor preço
        private BigDecimal melhorPreco;  // Melhor preço do agente acima
        private int repliesCnt = 0; //Contador de resposta (usado para calcular o números de agentes de venda)
        private MessageTemplate mt; // Template para receber as respostas
        private int step = 0;

        public void action() {
            System.out.println("Passo "+step);
            switch (step) {
                case 0:
                    System.out.println("\nVamos negociar!");
                    // Call For Proposal para todos os agentes de venda disponíveis
                    /**
                     * Vou resumir o que entendi do livro sobre o myAgent. Basicamente quando você for iniciar uma
                     * classe myAgent você é obrigado a ter um parâmetro ACLMessage, esse parâmetro é usado para
                     * iniciar o protocolo de comunicação. Citando o exemplo do livro: a classe ContractNetInitiator
                     * recebe a mensagem Call For Proposal para então enviar a mensagem para os agentes que estão na
                     * conversa, é por isso que a mensagem em si precisa de um ID e de um NOME, além de seu conteúdo,
                     * que a mensagem em si (String). As classes iniciadoras de protocolo suportam interações um-para-um
                     * e um-para-muitos, dependendo do número de receptores especificado na mensagem de iniciação.
                     * */
                    ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
                    for (int i = 0; i < agentesVendor.length; ++i) {
                        System.out.println("Adicionando contato de "+agentesVendor[i]);
                        cfp.addReceiver(agentesVendor[i]);
                    }
                    cfp.setContent(carro.getModelo());//Conteúdo da mensagem
                    cfp.setConversationId("negociar-carros");//Id da mensagem
                    cfp.setReplyWith("cfp"+System.currentTimeMillis()); //Tempo em millisegundos para criar chave única
                    System.out.println("Call For Proposal, chave gerada: "+cfp.getReplyWith());
                    myAgent.send(cfp);//Enviando a mensangem para todos os agentes rastreados no onTick()
                    // Organizar o template pra receber as propostas do vendedores
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("negociar-carros"),//Filtra mensagens que fazem parte do mesmo id
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));//Filtra mensagens que são respostas da mensagem enviada anteriormente
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
                            // Oferta gerada pelo vendedor, sendo o conteúdo da mensagem-resposta do vendedor
                            BigDecimal preco = new BigDecimal(reply.getContent());

                            //Em BigDecimal a comparação é um pouco diferente, você precisa
                            //usar o compareTo, para comparar PRECO com MELHORPRECO
                            //se essa comparação retornar -1, significa que PRECO é menor que MELHORPRECO
                            if (melhorVendedor == null || preco.compareTo(melhorPreco) < 0) {
                                System.out.println("\nUm carro com menor preço encontrado, valor: "+preco);
                                melhorPreco = preco;
                                melhorVendedor = reply.getSender();
                            }
                        }
                        repliesCnt++;
                        //Os agentesVendor foi instânciado lá no onTick(), se alguém entrou depois já era
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
                    /**
                     * A resposta segue praticamente a mesma receita da mensagem para abrir a conversa, só muda que
                     * agora você está informando que você vai aceitar a proposta da sua primeira requisição. Uma pequena
                     * diferença é que agora você terá somente um Receiver, e não precisa mais de um FOR para mandar para
                     * a turma toda de agentes vendedores, somente aquele que tem o melhor preço.
                     * */
                    ACLMessage mensagemCompra = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    mensagemCompra.addReceiver(melhorVendedor);
                    mensagemCompra.setContent(carro.getModelo());
                    mensagemCompra.setConversationId("negociar-carros");
                    mensagemCompra.setReplyWith("Pedido: "+System.currentTimeMillis());
                    myAgent.send(mensagemCompra);
                    // A mesma lógica de antes, cria o template para receber uma resposta com base na conversa
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
                            System.out.println("Houve uma falha na negociação parece que o carro já foi vendido");
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
    } //Fim da INNER CLASS


    public Carro menu(){
        while (ok) {
            System.out.println("\n\n\n\n\n\n\n\n\n\n");//limpar console, não achei um jeito mais bonito de fazer isso
            System.out.println("Qual marca de carro você deseja comprar?");
            carro.setMarca(info.nextLine().toUpperCase());
            System.out.println("Qual modelo dessa marca?");
            carro.setModelo(info.nextLine().toUpperCase());
            System.out.println("Qual o ano de fabricação?");
            String ano = info.nextLine();
            System.out.println("Quanto você pode pagar? (não use pontuação)");
            String preco = info.nextLine();

            if (ano.matches("^\\d+$") && preco.matches("^\\d+$")) {//REGEX
                carro.setAno(Integer.parseInt(ano));
                carro.setPreco(new BigDecimal(preco));//Não aceita ponto
            } else {
                System.out.println("Ano ou preço inválidos!");
                continue;
            }

            System.out.println("Então o que você procura é...");
            System.out.println("Marca: " + carro.getMarca());
            System.out.println("Modelo: " + carro.getModelo());
            System.out.println("Ano: " + carro.getAno());
            System.out.println("Preço: " + carro.getPreco() + ",00 reais");
            System.out.println("Digite 1 para prosseguir ou 2 para refazer a busca");
            ok = info.nextInt() >= 2;//Se for maior ou igual a 2 a variável recebe FALSE
        }
        System.out.println("Contactando vendedores, aguarde...");
        return carro;
    }
}