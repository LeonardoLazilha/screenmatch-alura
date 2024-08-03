package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repositorio;

    private List<Serie> series = new ArrayList<>();

    public Principal(SerieRepository repositorio) {
        this.repositorio = repositorio;
    }

    private Optional<Serie> serieBusca;

    private Serie serie;


    public void exibeMenu() {
        var opcao = -1;
        while(opcao != 0) {
            var menu = """
                    \n1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar série por título
                    5 - Buscar séries por ator
                    6 - Top 5 séries
                    7 - Buscar séries por categoria
                    8 - Filtrar séries por temporada e avaliação
                    9 - Buscar episódio por trecho
                    10 - Buscar top 5 séries
                    11 - LIMITE DE ANO
                    
                 
                   
                                    
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSerieporTitulo();
                    break;
                case 5:
                    buscarSerieporAtor();
                    break;
                case 6:
                    buscarTopSeries();
                    break;
                case 7:
                    buscarSeriesPorCategoria();
                    break;
                case 8:
                    filtrarSeriesPorTemporadaEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosDepoisDeUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;

                default:
                    System.out.println("Opção inválida");
            }
        }
    }


    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        //dadosSeries.add(dados);
        Serie serie = new Serie(dados);
        repositorio.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){

        System.out.println("Escolha uma série pelo nome: \n");
        listarSeriesBuscadas();
        System.out.println("\n");
        var nomeSerie = leitura.nextLine();
        //busca a partir do nome da serie que o user digitar
        Optional<Serie> serie = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

//        Optional<Serie> serie = series.stream()
//                .filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
//                .findFirst();

        //verificar se existe

        if (serie.isPresent()) {

            var serieEncontrada = serie.get();

            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            //settar eps que existe na serie

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repositorio.save(serieEncontrada);

            temporadas.forEach(System.out::println);
        }else {
            System.out.println("Série não encontrada! :(");
        }

    }

    private void listarSeriesBuscadas(){
        series = repositorio.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }


    private void buscarSerieporTitulo(){
        System.out.println("Escolha uma série pelo título: ");
        var nomeSerie = leitura.nextLine();
        serieBusca = repositorio.findByTituloContainingIgnoreCase(nomeSerie);

        if (serieBusca.isPresent()){
            System.out.println("Dados da série: " + serieBusca.get());
        }else {
            System.out.println("Série não encontrada! ; (");
        }
    }

    private void buscarSerieporAtor(){
        System.out.println("Informe o nome do ator: ");
        var nomeAtor = leitura.nextLine();
        System.out.println("avaliação a partir de qual valor?");
        var avalicao = leitura.nextDouble();
        List <Serie> seriesEncontradas = repositorio.findByAtoresContainingIgnoreCaseAndAvaliacaoGreaterThanEqual(nomeAtor, avalicao);
        System.out.println("Séries que " + nomeAtor.toUpperCase() + " atuou:");
        seriesEncontradas.forEach( serie -> System.out.println(serie.getTitulo() + " - avaliação: " + serie.getAvaliacao()));
        System.out.println(seriesEncontradas);
    }

    private void buscarTopSeries(){
        List<Serie> serieTop = repositorio.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach( s -> System.out.println(s.getTitulo() + " - avaliação: " + s.getAvaliacao()));
        System.out.println(serieTop);
    }

    private void buscarSeriesPorCategoria() {
        System.out.println("Deseja buscar séries por qual gênero/categoria? ");
        var nomeGenero = leitura.nextLine();
        try {
            Categoria categoria = Categoria.fromPortugues(nomeGenero);
            List<Serie> seriesPorCategoria = repositorio.findByGenero(categoria);
            System.out.println("Séries da categoria " + nomeGenero);
            seriesPorCategoria.forEach(System.out::println);
        } catch (IllegalArgumentException e) {
            System.err.println("Erro: " + e.getMessage());
        }
    }

    private void filtrarSeriesPorTemporadaEAvaliacao(){
        System.out.println("Filtrar séries até quantas temporadas? ");
        var totalTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("Com avaliação a partir de que valor? ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> filtroSeries = repositorio.seriesPorTemporadaEAvalicao(totalTemporadas, avaliacao);
        System.out.println("\n*** Séries filtradas ***");
        filtroSeries.forEach(s ->
                System.out.println(s.getTitulo() + "  - avaliação: " + s.getAvaliacao()));
    }


    private void buscarEpisodioPorTrecho() {
        System.out.println("Qual o nome do episódio para busca? ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repositorio.episodiosPorTrecho(trechoEpisodio);
        //episodiosEncontrados.forEach(System.out::println);
        episodiosEncontrados.forEach(e ->
                System.out.printf("Série: %s Temporada %s - Episódio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo())
        );
    }

    private void topEpisodiosPorSerie(){
        buscarSerieporTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repositorio.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s - Avaliação: %.1f\n",
                            e.getSerie().getTitulo(), e.getTemporada(), e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao()));

        }
    }

    private void buscarEpisodiosDepoisDeUmaData() {
        buscarSerieporTitulo();
        if (serieBusca.isPresent()){
            System.out.println("Digite o limite do ano de lançamento: ");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();



            List<Episodio> episodiosAno = repositorio.episodiosPorSerieEAno(serie, anoLancamento);
            episodiosAno.forEach(System.out::println);
        }
    }


}