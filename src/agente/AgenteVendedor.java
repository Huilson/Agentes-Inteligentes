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
        // ou insira um valor de 0 a 4 para gerar um carro específico.
        adicionarCarros(1);//adiciona um carro ao vendedor.
        //adicionarCarros(1);//esse segundo carro é só para gerar 2 carros iguais, mas com preços diferentes.
        //Por que? Para testar o algoritmo mesmo

        /**
         * Registro do serviço do vendedor de carros nas páginas amarelas.
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
    private class OfertarCarro extends CyclicBehaviour {
        public void action() {
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);// Call for Proposal, da para dizer
            //que isso funciona quase como um anúncio no mercado, mas aqui ele fica aguardando alguém chamar
            ACLMessage msg = myAgent.receive(mt);// Resposta da mensagem, caso um comprador chame
            if (msg != null) {
                // Um(s) comprador recebeu a call
                String prospostaComprador = msg.getContent();// Conteúdo da Mensagem, é o que o comprador deseja comprar

                String[] verCarro = prospostaComprador.split("-");
                String modelo = verCarro[0];
                String valor = verCarro[1];
                int quantidade = Integer.parseInt(verCarro[2]);

                System.out.println("Resposta de um comprador recebida, ele deseja um: " + modelo);
                System.out.println("Valor máximo que ele pode pagar: " + valor);
                System.out.println("Quantos quer: " + quantidade);
                ACLMessage resposta = msg.createReply();// Resposta da mensagem, se tem ou não o que o comprador quer, vai ser escrito aqui

                // Primeiro precisa ver se tem o carro que o comprador quer
                Carro carro = buscarCarro(modelo, valor);

                if (carro != null && carro.getQuantidade() >= quantidade) {
                    // O carro requerido está disponível para venda
                    resposta.setPerformative(ACLMessage.PROPOSE);
                    resposta.setContent(carro.getPreco().toString());//retorna só o preço do carro
                } else {
                    // O carro requerido NÃO está disponível para venda
                    System.out.println("Não tenho essa quantidade de carro nesse modelo: " + quantidade);
                    resposta.setPerformative(ACLMessage.REFUSE);
                    resposta.setContent("carros insuficientes");
                }
                myAgent.send(resposta);//envia a resposta ao(s) compradore(s)
            } else {
                block();
            }
        }
    }  // End of inner class Ofertar Carro

    private class VenderCarro extends CyclicBehaviour {
        public void action() {
            /**
             * Mesma lógica de sempre, cria o template etc, etc, etc...
             * Aqui, porém a troca de mensagens será referente ao desconto solicitado pelo
             * comprador, isso leva um certo tempo, precisa verificar bastante coisa durante
             * o processo de desconto e de venda.
             */
            MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
            ACLMessage modeloDesejado = myAgent.receive(mt);
            if (modeloDesejado != null) {
                System.out.println("Recebemos uma proposta de desconto... Vamos ver se o carro ainda esta disponível");
                //Busca pelo carro desejado, vai que já vendeu
                Carro carro = buscarCarro(modeloDesejado.getContent());
                // Precisa responder ao amigo se ainda dá para pedir desconto
                ACLMessage confirmarDesconto = modeloDesejado.createReply();

                if (carro != null) {
                    System.out.println("Continuando...");
                    // Concorda em dar desconto
                    confirmarDesconto.setPerformative(ACLMessage.AGREE);
                    confirmarDesconto.setContent("ok");
                } else {
                    System.out.println("Não temos mais o carro");
                    // O carro requerido NÃO está mais disponível para venda
                    confirmarDesconto.setPerformative(ACLMessage.REFUSE);
                    confirmarDesconto.setContent("erro");
                }
                myAgent.send(confirmarDesconto);//envia a resposta ao(s) compradore(s)

                try {
                    Thread.sleep(5000);//Esperar chamado do desconto
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

                // Mesma lógica da oferta
                mt = MessageTemplate.MatchPerformative(ACLMessage.QUERY_IF);// O Query_If é usado para pedir se tem algo
                ACLMessage pedidoDeDesconto = myAgent.receive(mt);
                System.out.println(pedidoDeDesconto);
                /**
                 * A resposta aqui é bem grande, vem os adicionais e o ano do carro,
                 * precisa ter cuidado na hora de montar e separar a STRING!
                 * */
                if (pedidoDeDesconto != null) {
                    ACLMessage enviarDesconto = pedidoDeDesconto.createReply();//cria resposta

                    System.out.println("Vamos calcular o desconto...");
                    // Remove colchetes
                    String resposta = pedidoDeDesconto.getContent().replace("[", "").replace("]", "");

                    // Divide por vírgula (Se fosse em Kotlin dava para usar o After e o Before... Pobre Java...)
                    // Enfim, Regex pra que te quero e vida que segue!
                    String[] particao = resposta.split("ano-");
                    String anoDesejado = particao[1];
                    String[] adicionais = resposta.split(",\\s*"); // Esse "\\s*" ignora espaços depois da vírgula

                    //Essa correção remove o "-ano????" do último adicional que veio na mensagem
                    String correcao = adicionais[adicionais.length - 1];//pega o tamanho de caracteres do último adicional
                    adicionais[adicionais.length - 1] = correcao.substring(0, correcao.length() - 8);//Mato os últimos 8 caracteres

                    int desconto = 10;// Começa com 10% de desconto

                    for (String adicional : adicionais) {
                        if (carro.getAdicionais().toString().contains(adicional)){
                            System.out.println("Adicional encontrado: " + adicional);
                            desconto--;// Para cada adicional perde 1% de desconto
                        }
                    }

                    System.out.println("Ano que o comprador deseja o carro: " + anoDesejado);
                    System.out.println("O que temos aqui: " + carro.getAno());
                    int anoCarro = Integer.parseInt(anoDesejado);
                    if (anoCarro > carro.getAno()) {
                        desconto += anoCarro - carro.getAno();// Se o vendedor tiver um carro muito velho
                        //o comprador ganha 1% de desconto para cada ano de diferença
                    }

                    if (desconto > 0) {
                        System.out.println("Tudo bem, vou te dar um desconto de: " + desconto);
                        BigDecimal percentualDesconto = new BigDecimal(desconto);
                        BigDecimal calculoDesconto = BigDecimal.ONE.subtract(percentualDesconto.divide(
                                new BigDecimal("100"), 4, RoundingMode.HALF_UP));
                        BigDecimal precoComDesconto = carro.getPreco().multiply(calculoDesconto);

                        enviarDesconto.setPerformative(ACLMessage.AGREE);//Aceita dar o desconto
                        enviarDesconto.setContent(precoComDesconto.toString());
                    } else {
                        // Não vai dar desconto
                        System.out.println("Desculpe, não posso te dar desconto...");
                        enviarDesconto.setPerformative(ACLMessage.CANCEL);//Recusa em dar o desconto
                        enviarDesconto.setContent("0");
                    }
                    myAgent.send(enviarDesconto);
                } else {
                    block();
                }
            }

            /**
             * FINALMENTE! Depois de muita negociação o carro pode ser vendido!
             * */
            //mesma lógica de sempre
            mt = MessageTemplate.MatchPerformative(ACLMessage.CONFIRM);
            ACLMessage msg = myAgent.receive(mt);
            if (msg != null) {
                // Aqui o comprador aceitou a proposta
                String prospostaComprador = msg.getContent();

                String[] particao = prospostaComprador.split("-");
                String modelo = particao[0];
                int ano = Integer.parseInt(particao[1]);

                ACLMessage reply = msg.createReply();

                /** Durante a troca de mensagens pode acontecer do vendedor ter vendido o carro para
                 * outro comprador, por isso antes de INFORMAR o comprador que o carro será dele é
                 * preciso buscar o carro novamente, se ele existir remove o carro INFORMA que a
                 * compra deu certo, mas e o conteúdo da mensagem? Daí tem que ver, pode ser o carro
                 * em si ou qualquer outra coisa, nesse nosso código não precisa passar nada, afinial
                 * é só um faz de conta, mas se quiser dá para passar um conteúdo na mensagem.
                 */
                BigDecimal preco = removerCarro(modelo, ano);
                if (preco != null) {
                    reply.setPerformative(ACLMessage.INFORM);//Informar que carro foi vendido
                    System.out.println(prospostaComprador + " vendido para " + msg.getSender().getName());
                } else {
                    // O carro foi vendido para outro comprador enquanto o tramite ocorria.
                    reply.setPerformative(ACLMessage.FAILURE);
                    reply.setContent("Opa, parece que não há mais carros...");
                }
                myAgent.send(reply);
            } else {
                block();
            }
        }
    }  // End of inner class Vender Carro

    /**
     * FUNÇÕES DIVERSAS
     */
    private Carro buscarCarro(String carroRequisitado, String valorRequsitado) {
        for (Carro carro : carros) {//Itera pelos carros da lista
            BigDecimal preco = new BigDecimal(valorRequsitado);

            if (carro.getModelo().equals(carroRequisitado) && carro.getPreco().compareTo(preco) <= 0) {//por enquanto só busca pelo modelo
                System.out.println("Carro encontrado, podemos negociar!");
                return carro;//encontrou o carro
            }
        }//fim for
        return null;//carro não encontrado
    }

    /**BUSCA PARA DESCONTO*/
    private Carro buscarCarro(String carroRequisitado) {
        for (Carro carro : carros) {//Itera pelos carros da lista

            if (carro.getModelo().equals(carroRequisitado)) {//por enquanto só busca pelo modelo
                System.out.println("Carro encontrado, hora de ver o desconto!");
                return carro;//encontrou o carro
            }
        }//fim for
        return null;//carro não encontrado
    }

    /**FUNÇÃO DE BUSCA PARA REMOÇÃO*/
    private Carro buscarCarro(String carroRequisitado, int quantidade) {
        for (Carro carro : carros) {//Itera pelos carros da lista
            if ( carro.getModelo().equals(carroRequisitado) && carro.getQuantidade() >= quantidade) {//por enquanto só busca pelo modelo
                System.out.println("Carro encontrado, vamos vender");
                return carro;//encontrou o carro
            }
        }//fim for
        return null;//carro não encontrado
    }

    private BigDecimal gerarValor(double precoMinino, double precoMaximo) {
        BigDecimal preco = new BigDecimal(
                new Random().nextDouble(precoMaximo - precoMinino) + precoMinino
        );
        return preco.setScale(2, RoundingMode.HALF_UP);
    }

    public void adicionarCarros(int index) {
        Carro onix = new Carro(
                "ONIX",
                "Chevrolet",
                2025,
                3,
                List.of(Adicionais.AR_CONDICIONADO, Adicionais.CAMARA_RE, Adicionais.DIRECAO_HIDRAULICA),
                100,
                gerarValor(90000.00, 95000.00),
                1
        );
        Carro cruze = new Carro(
                "CRUZE",
                "Chevrolet",
                2020,
                2,
                List.of(Adicionais.AR_CONDICIONADO, Adicionais.CAMARA_RE, Adicionais.DIRECAO_HIDRAULICA,
                        Adicionais.CENTRAL_MULTIMIDIA, Adicionais.VIDROS_ELETRICOS),
                100,
                gerarValor(80000.00, 95000.00),
                2
        );
        Carro gol = new Carro(
                "GOL",
                "Volkswagen",
                2010,
                1,
                List.of(Adicionais.BANCOS_DE_COURO),
                100,
                gerarValor(10000.00, 15000.00),
                1
        );
        Carro pollo = new Carro(
                "POLLO",
                "Volkswagen",
                2025,
                3,
                List.of(Adicionais.CAMARA_RE, Adicionais.AR_CONDICIONADO, Adicionais.CENTRAL_MULTIMIDIA,
                        Adicionais.SISTEMA_DE_COLISAO, Adicionais.VIDROS_ELETRICOS),
                100,
                gerarValor(75000.00, 89290.00),
                3
        );
        Carro mustangGT = new Carro(
                "MUSTANG GT",
                "Ford",
                2025,
                3,
                List.of(Adicionais.CAMARA_RE, Adicionais.AR_CONDICIONADO, Adicionais.CENTRAL_MULTIMIDIA,
                        Adicionais.SISTEMA_DE_COLISAO, Adicionais.VIDROS_ELETRICOS),
                100,
                gerarValor(45000.00, 549000.00),
                1
        );

        switch (index) {
            case 0:
                System.out.println("O vendedor tem um Onix disponível, preço: " + onix.getPreco() + "!");
                carros.add(onix);
                break;
            case 1:
                System.out.println("O vendedor tem um Cruze disponível, preço: " + cruze.getPreco() + "!");
                carros.add(cruze);
                break;
            case 2:
                System.out.println("O vendedor tem um Gol disponível, preço: " + gol.getPreco() + "!");
                carros.add(gol);
                break;
            case 3:
                System.out.println("O vendedor tem um Pollo disponível, preço: " + pollo.getPreco() + "!");
                carros.add(pollo);
                break;
            case 4:
                System.out.println("O vendedor tem um Mustang GT disponível, preço: " + mustangGT.getPreco() + "!");
                carros.add(mustangGT);
                break;
        }

    }

    private BigDecimal removerCarro(String carroVendido, int quantidade){
        Carro carro = buscarCarro(carroVendido, quantidade);//Chama novamente a Função de busca
        if (carro != null) {
            carro.setQuantidade(carro.getQuantidade() - quantidade);
            if(carro.getQuantidade() <= 0){
                carros.remove(carro);//Remove o carro que foi vendido da lista
            }else{
                return null;
            }
        }
        if (carro != null) {
            return carro.getPreco();//Retorna o preço do carro
        }
        return null;//se não achou o carro retorna nulo
    }

}//FIM DA CLASSE MAIN