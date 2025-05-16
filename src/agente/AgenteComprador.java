/**
 * CODED BY: HUILSON JOSÉ LORENZI, VINICÍUS BAUER
 *
 * LEIA: ANTES DE COMEÇAR A RODAR VEJA O AGENTEVENDEDOR E ORGANIZE OS CARROS DE CADA VENDEDOR
 * MODELO, ADICIONAIS, QUANTIDADE, VALOR, ETC...
 * DEPOIS PODE RODAR O CÓDIGO ELE ESTÁ BEM COMENTADO, CUIDADO PARA NÃO ERRAR NA HORA DE DIGITAR
 * OS DADOS DO CARRO TANTO NO COMPRADOR COMO NO VENDEDOR. O ALGORITMO IRÁ VER QUAL VENDEDOR
 * TEM O CARRO MAIS BARATO DEPOIS VAI TENTAR CRIAR UM DESCONTO COM BASE NO ANO E NOS ADICIONAIS
 * DO CARRO, POR FIM SE O CARRO ESTIVER DISPONÍVEL ELE SERÁ VENDIDO.
 * DEPOIS DE UMA PEQUISA PERCEBI QUE SEQUENTIALBEHAVIOR ERA O MELHOR PARA A IDEIA DO ALGORITMO
 * DEI UMA LIDA E IMPLEMENTEI, ESPERO QUE TENHA FICADO BOM.
 *
 * SINTO TAMBÉM POR NÃO TER TESTADO COM VÁRIOS COMPRADORES, PODE SER QUE O CÓDIGO APRESENTE
 * FALHAS, MAS 1 COMPRADOR PARA MUITOS VENDEDORES (3 NO CASO) FOI TRANQUILO, NÃO FORCEI MUITO O
 * CÓDIGO, MAS ACREDITO QUE NÃO TENHA PROBLEMAS.
 *
 * OUTRA COISA, NÃO APAGUE OS SLEEP, SE NÃO DÁ PAU KKKKKKK
 * SEI QUE É CHATO FICAR ESPERANDO, MAS SÉRIO, NÃO MEXA NOS SLEEPS
 * */

package agente;

import jade.core.AID;
import jade.core.Agent;
import jade.core.behaviours.*;
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
import java.util.List;
import java.util.Scanner;

public class AgenteComprador extends Agent {

    private int step = 0;
    Carro carro = new Carro();
    Scanner info = new Scanner(System.in);
    boolean ok = true;// Usado no REGEX para converter String para número (int e BigDecimal)

    private AID[] agentesVendor;// Usado para criar uma lista de agentes de venda disponíveis.

    @Override
    protected void setup() {
        carro = menu();
        addBehaviour(new SimpleBehaviour() {
            private boolean finalizado = false;

            public void action() {
                System.out.println("Requisitando compra... " + carro.getModelo());
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

                /**
                 * Segundo o livro, se um agente deseja publicar um ou mais serviço é preciso ter o DF
                 * com a descrição que inclui o AID do agente, uma lista de serviços, e se quiser uma lista
                 * de linguagens e modelos que outros agentes podem usar para interagir.
                 */
                try {
                    DFAgentDescription[] result = DFService.search(myAgent, template);
                    System.out.println("Agentes de venda encontrados:");
                    agentesVendor = new AID[result.length];//tamanho do array de agentes encontrados
                    for (int i = 0; i < result.length; ++i) {
                        agentesVendor[i] = result[i].getName();
                        System.out.println(agentesVendor[i].getName());
                    }
                } catch (FIPAException fe) {
                    fe.printStackTrace();
                }

                // Comportamento para fazer as requisições de compra
                myAgent.addBehaviour(new NegociarCarro(myAgent));
                //addBehaviour(new NegociarCarro(this));
                finalizado = true;
            }

            public boolean done() {
                System.out.println("Hora de buscar os vendedores...\n\n\n");
                return finalizado;
            }
        });
    }

    protected void takeDown() {
        System.out.println("O agente " + getAID().getName() + " terminou o serviço e agora será encerrado.");
    }

    /**
     * INNER CLASS
     */
    private class NegociarCarro extends Behaviour {

        SequentialBehaviour seq = new SequentialBehaviour();
        private boolean finalizado = false;

        private AID melhorVendedor; // Armazena o agente de venda com o melhor preço
        private BigDecimal melhorPreco;  // Melhor preço do agente acima
        private int repliesCnt = 0; //Contador de resposta (usado para calcular o números de agentes de venda)
        private MessageTemplate mt; // Template para receber as respostas

        public NegociarCarro(Agent agent) {
            super(agent);
            seq = new SequentialBehaviour() {

                public int onEnd() {
                    finalizado = true;  // Marca como finalizado quando termina
                    return super.onEnd();
                }
            };

            seq.addSubBehaviour(new OneShotBehaviour(agent) {
                public void action() {
                    System.out.println("\nPasso 1");
                    System.out.println("Vamos negociar!");
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
                    System.out.println("Número de vendedores encontrado: " + agentesVendor.length);
                    for (int i = 0; i < agentesVendor.length; ++i) {
                        System.out.println("Adicionando contato de " + agentesVendor[i]);
                        cfp.addReceiver(agentesVendor[i]);
                    }
                    cfp.setContent(carro.getModelo()+"-"+carro.getPreco()+"-"+carro.getQuantidade());//Conteúdo da mensagem
                    cfp.setConversationId("negociar-carros");//Id da mensagem
                    cfp.setReplyWith("cfp" + System.currentTimeMillis()); //Tempo em millisegundos para criar chave única
                    System.out.println("Call For Proposal, chave gerada: " + cfp.getReplyWith());
                    agent.send(cfp);//Enviando a mensangem para todos os agentes rastreados no onTick()
                    // Organizar o template pra receber as propostas do vendedores
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("negociar-carros"),//Filtra mensagens que fazem parte do mesmo id
                            MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));//Filtra mensagens que são respostas da mensagem enviada anteriormente
                    step = 1;
                    try {
                        Thread.sleep(5000);//dar um tempinho para ler o console
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    //break;
                }
            });
            seq.addSubBehaviour(new OneShotBehaviour(agent) {
                public void action() {
                    System.out.println("\nPasso 2");
                    // Recebe todas as respostas dos vendedores (seja ACEITE ou RECUSA)
                    ACLMessage resposta = agent.receive(mt);
                    if (resposta != null) {
                        // Resposta de um vendedor, se for uma PROPOSTA toca o baile
                        if (resposta.getPerformative() == ACLMessage.PROPOSE) {
                            // Oferta gerada pelo vendedor, sendo o conteúdo da mensagem-resposta do vendedor
                            BigDecimal preco = new BigDecimal(resposta.getContent());

                            //Em BigDecimal a comparação é um pouco diferente, você precisa
                            //usar o compareTo, para comparar PRECO com MELHORPRECO
                            //se essa comparação retornar -1, significa que PRECO é menor que MELHORPRECO
                            if (melhorVendedor == null || preco.compareTo(melhorPreco) < 0) {
                                System.out.println("Um carro com menor preço encontrado, valor: " + preco);
                                melhorPreco = preco;
                                melhorVendedor = resposta.getSender();
                                System.out.println("O vendedor é: " + melhorVendedor + "\n");
                            }
                        } else if (resposta.getPerformative() == ACLMessage.REFUSE) {
                            System.out.println("O vendedor "+resposta.getSender()+" não tem carros suficientes, ou não tem o carro que você deseja");
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
                    } else {
                        block();
                    }
                }
            });
            seq.addSubBehaviour(new OneShotBehaviour(agent) {
                public void action() {
                    System.out.println("Passo 3");
                    /**
                     * A resposta segue praticamente a mesma receita da mensagem para abrir a conversa, só muda que
                     * agora você está informando que você vai aceitar a proposta da sua primeira requisição. Uma pequena
                     * diferença é que agora você terá somente um Receiver, e não precisa mais de um FOR para mandar para
                     * a turma toda de agentes vendedores, somente aquele que tem o melhor preço.
                     * */
                    System.out.println("Vamos tentar fechar o acordo com " + melhorVendedor + "!");
                    System.out.println("Mas antes que tal tentar algum desconto?");
                    //Informa novamente o carro desejado
                    ACLMessage mensagemCompra = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
                    mensagemCompra.setContent(carro.getModelo());
                    mensagemCompra.setConversationId("negociar-carros");
                    mensagemCompra.setReplyWith("Pedido: " + System.currentTimeMillis());
                    mensagemCompra.addReceiver(melhorVendedor);
                    agent.send(mensagemCompra);
                    System.out.println("Verificando se o carro ainda está disponível, aguarde...");
                    mt = MessageTemplate.and(MessageTemplate.MatchConversationId("negociar-carros"),
                            MessageTemplate.MatchInReplyTo(mensagemCompra.getReplyWith()));
                    try {
                        Thread.sleep(3000);//esperar 3 segundos antes de continuar
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    step = 3;
                }
            });
            seq.addSubBehaviour(new OneShotBehaviour(agent) {
                @Override
                public void action() {
                    System.out.println("\nPasso 4");
                    // Hora de aguardar a resposta; para ver se tem desconto, o desconto vai ser possível?
                    // Template da resposta para ver se vendedor concordou em dar o desconto
                    //MessageTemplate template = MessageTemplate.MatchPerformative(ACLMessage.AGREE);

                    if (agent.receive().getPerformative() == ACLMessage.AGREE) {
                        System.out.println("Carro ainda disponível! Vamos calcular o desconto, aguarde...");
                        /**
                         * Aqui é montado a STRING para ver o desconto
                         * favor tomar cuidado ao mexer aqui, pois a STRING é quebrada e separada na outra ponta,
                         * qualquer alteração pode quebrar o código!
                         * */
                        String mensagemDesconto = carro.getAdicionais().toString()
                                + "ano-" + String.valueOf(carro.getAno());


                        ACLMessage desconto = new ACLMessage(ACLMessage.QUERY_IF);//O Query_If é usado para pedir se tem algo
                        desconto.addReceiver(melhorVendedor);
                        desconto.setContent(mensagemDesconto);
                        desconto.setConversationId("negociar-carros");
                        desconto.setReplyWith("Pedido: " + System.currentTimeMillis());
                        agent.send(desconto);
                        try {
                            Thread.sleep(15000);//Esperar calculo do desconto
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }

                        // Hora de aguardar o valor do desconto... Se é que vai ter...
                        MessageTemplate templateValorDesconto = MessageTemplate.MatchPerformative(ACLMessage.AGREE);
                        ACLMessage valorDesconto = agent.receive(templateValorDesconto);
                        if(valorDesconto != null){
                            if (!valorDesconto.getContent().equals("0")) {
                                System.out.println("Maravilha! Conseguimos um desconto de: " + valorDesconto.getContent());
                                carro.setPreco(new BigDecimal(valorDesconto.getContent()).setScale(2, RoundingMode.HALF_UP));
                                System.out.println("Agora você vai pagar: " + carro.getPreco());
                            } else {
                                System.out.println("Infelizmente o vendedor não deu nenhum desconto...");
                            }
                        } else {
                            block();
                        }
                    } else {
                        System.out.println("Parece que algo deu errado, o carro provavelmente já foi vendido...");
                        System.out.println("Tente executar um novo agente e buscar por um novo carro.");
                        agent.doDelete();//mata o agente, vai ter de recomeçar...
                        block();
                    }
                    step = 4;
                }
            });
            seq.addSubBehaviour(new OneShotBehaviour(agent) {
                public void action() {
                    System.out.println("\nPasso 5");
                    System.out.println("Agora vamos confirmar a compra");

                    ACLMessage comprarCarro = new ACLMessage(ACLMessage.CONFIRM);// O INFORM é usado para passar informações
                    comprarCarro.setContent(carro.getModelo()+"-"+carro.getQuantidade());
                    comprarCarro.setConversationId("negociar-carros");
                    comprarCarro.setReplyWith("Pedido: " + System.currentTimeMillis());
                    comprarCarro.addReceiver(melhorVendedor);
                    agent.send(comprarCarro);
                    try {
                        Thread.sleep(5000);//Esperar calculo do desconto
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                    mt = MessageTemplate.MatchPerformative(ACLMessage.INFORM);
                    ACLMessage resposta = agent.receive(mt);
                    if (resposta != null) {
                        // Se recebeu a mensagem imforma que deu tudo certo
                        if (resposta.getPerformative() == ACLMessage.INFORM) {
                            // Compra bem sucedida
                            System.out.println(carro.getModelo() + " foi comprado do vendedor " + resposta.getSender().getName());
                            System.out.println("Preço = " + melhorPreco);
                            System.out.println("Parabéns pela sua nova conquista!");
                            agent.doDelete();//encerrando o agente
                        } else {
                            System.out.println("Houve uma falha na negociação parece que o carro já foi vendido");
                        }

                        step = 5;
                        try {
                            Thread.sleep(5000);//dar um tempinho para ler o console
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                    } else {
                        block();
                    }
                }
            });
        }

        @Override
        public void action() {
            // Executa o SequentialBehaviour como parte desse Behaviour
            if (!finalizado) {
                seq.action();
            }
        }

        @Override
        public boolean done() {
            if (step == 2 && melhorVendedor == null) {
                System.out.println("Não foi possível encontrar o : " + carro.getModelo() + " para vender!");
                myAgent.doDelete();
            }
            return ((step == 2 && melhorVendedor == null) || step == 5);
        }
    }//Fim da INNER CLASS

    /**
     * FUNÇÕES DIVERSAS
     */
    protected Carro menu() {
        System.out.println("Por favor, antes de começar inicie os agentes de venda!!!");
        while (ok) {
            System.out.println("\n\n\n");//limpar console, não achei um jeito mais bonito de fazer isso
            System.out.println("Qual marca de carro você deseja comprar?");
            carro.setMarca(info.nextLine().toUpperCase());
            System.out.println("Qual modelo dessa marca?");
            carro.setModelo(info.nextLine().toUpperCase());
            System.out.println("Qual o ano de fabricação?");
            String ano = info.nextLine();
            System.out.println("Quantos quer?");
            String qnt = info.nextLine();
            System.out.println("Quanto você pode pagar? (não use pontuação)");
            String preco = info.nextLine();

            if (ano.matches("^\\d+$") && preco.matches("^\\d+$") && qnt.matches("^\\d+$")) {//REGEX
                carro.setQuantidade(Integer.parseInt(qnt));
                carro.setAno(Integer.parseInt(ano));
                carro.setPreco(new BigDecimal(preco));//Não aceita ponto
            } else {
                System.out.println("Ano, quantidade ou preço inválidos!");
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
        //gerando adicionais...
        carro.setAdicionais(List.of(Adicionais.AR_CONDICIONADO, Adicionais.CAMARA_RE, Adicionais.BANCOS_DE_COURO,
                Adicionais.VIDROS_ELETRICOS, Adicionais.CENTRAL_MULTIMIDIA, Adicionais.DIRECAO_HIDRAULICA,
                Adicionais.FAROL_DE_NEBLINA, Adicionais.SISTEMA_DE_COLISAO, Adicionais.SISTEMA_SOCORRO,
                Adicionais.PILOTO_AUTOMATICO));
        System.out.println("Contactando vendedores, aguarde...");
        return carro;
    }
}