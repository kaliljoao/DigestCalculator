import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DigestCalculator {

    private static ArrayList<String> pathFiles; // path da pasta com os arquivos a serem processados
    private static ArrayList<String[]> listDigests; //lista de String[] para que possa ler o arquivo em partes
    private static ArrayList<String[]> listDigestsCopyToWrite = new ArrayList<String[]>();
    private static MessageDigest myDigest;
    private static Path myPath;

    private static enum Status {
        UNKNOW,
        OK,
        NOT_OK,
        NOT_FOUND,
        COLLISION,
    }

    public static void TypeDigest(String type) throws NoSuchAlgorithmException { // processa Tipo_Digest podendo ser MD5 ou SHA1
        myDigest = MessageDigest.getInstance(type);
        System.out.println(myDigest.getProvider());
        System.out.println("-------------------------------------------------------");
    }

    public static void setPath(String pathArqListaDigest) throws IOException { // processa Caminho_ArqListaDigest
        myPath = Paths.get(pathArqListaDigest);
        listDigests = new ArrayList<String[]>();

        List<String> lines = Files.readAllLines(myPath);
        System.out.println("\n------------------------ Cada linha do arquivo: ------------------------");

        if (lines.size() > 0) {
            for (String line : lines) {
                listDigests.add(line.split(" "));
            }

            listDigests.forEach(lineInformation -> {
                System.out.println(Arrays.toString(lineInformation));
            });
        }
        System.out.println("-------------------------------------------------------");

        Files.readAllLines(myPath).forEach(lineInformation -> {
            listDigests.add(lineInformation.split(" "));
        });


    }

    public static void setFileList(String Path) { // processa Caminho_da_Pasta_dos_Arquivos
        pathFiles = new ArrayList<String>();
        final File folder = new File(Path);
        System.out.println("\n---------------------- PATHS ----------------------");
        for (final File file : folder.listFiles()) {
            pathFiles.add(file.getPath());

            System.out.println(file.getPath());
        }
        System.out.println("-------------------------------------------------------");

    }

    private static String DigestValueFromFile(Path filePath) throws IOException {
        if (Files.exists(filePath) == false) {
            System.err.print("FILE DOESN'T EXIST, EXITING \n");
            System.exit(2);
        }

        String digestHexa = "";

        byte[] fileBytes = Files.readAllBytes(filePath);
        myDigest.update(fileBytes, 0, fileBytes.length);
        fileBytes = myDigest.digest();

        for (int i = 0; i < fileBytes.length; i++)
            digestHexa = digestHexa + String.format("%02X", fileBytes[i]);

        return digestHexa;
    }

    private static String DigestFromDigestsFile(int index) throws Exception {
        String[] information = listDigests.get(index);
        for (int i = 1; i < information.length; i = i + 2) {
            String type = information[i];

            if (type.equalsIgnoreCase(myDigest.getAlgorithm()))
                return information[i + 1];
        }

        return null;
    }

    private static Status FileStatus(String fileName, String fileDigest) throws Exception {
        Status status = Status.UNKNOW;

        // Confere digest com os arquivos passados
        for (int i = 0; i < pathFiles.size(); i++) {
            Path pathArq = Paths.get(pathFiles.get(i));
            String arqName = pathArq.getFileName().toString();

            if (fileName.equals(arqName)) // ignora o proprio arquivo
                continue;

            if (fileDigest.equalsIgnoreCase(DigestValueFromFile(pathArq))) {
                status = Status.COLLISION;
            }
        }

        // Confere digest com lista de digests
        for (int i = 0; i < listDigests.size(); i++) {
            String nameFileFromDigestsFile = listDigests.get(i)[0];
            String digestFromFileOfDigestsFile = DigestFromDigestsFile(i);

            if (fileName.equals(nameFileFromDigestsFile) && status == Status.UNKNOW) {
                if (fileDigest.equals(digestFromFileOfDigestsFile)) {
                    status = Status.OK;
                } else if (digestFromFileOfDigestsFile != null) {
                    status = Status.NOT_OK;
                }
            } else if (fileDigest.equalsIgnoreCase(digestFromFileOfDigestsFile)) {
                status = Status.COLLISION;
            }
        }

        if (status == Status.UNKNOW)
            status = Status.NOT_FOUND;

        return status;
    }

    public static void AddNovoDigest(String name, String digest, String alg) throws Exception {

        String[] novaInf = new String[3];

        for (int j = 0; j < listDigests.size(); j++) {
            String n = listDigests.get(j)[0];

            if (name.equals(n) == false) // se nao for o arquivo que eu quero continuo
                continue;

            String[] original = listDigests.get(j);
            String[] novoDigest = new String[original.length + 2];

            for (int f = 0; f < original.length; f++)
                novoDigest[f] = original[f]; // pega a informacao anterior

            novoDigest[original.length] = alg;
            novoDigest[original.length + 1] = digest; // adiciona nova informacao

            listDigestsCopyToWrite.add(novoDigest);

            return;
        }

        novaInf[0] = name;
        novaInf[1] = alg;
        novaInf[2] = digest;
        listDigestsCopyToWrite.add(novaInf);
        return;
    }


    public static void Verify() throws Exception {

        System.out.println("\n------------------------- INICIANDO A VERIFICACAO DOS ARQUIVOS ----------------------------\n");

        boolean notfound=false;
        Path file ;
        String fileName = null;
        String fileDigest ;
        String alg;
        Status status ;

        for (int i = 0; i < pathFiles.size(); i++) {



            file = Paths.get(pathFiles.get(i));
            fileName = file.getFileName().toString();
            fileDigest = DigestValueFromFile(file);
            alg = myDigest.getAlgorithm();
            status = FileStatus(fileName, fileDigest);

            System.out.printf("%s %s %s %s\n", fileName, alg, fileDigest, status.toString());

            if (status == Status.NOT_FOUND) {
                notfound = true;
                AddNovoDigest(fileName, fileDigest, alg);
            }

        }

        if(notfound) // se algum digest não foi achado na lista de digests
            EscreverNoArq();


        System.out.println("\n------------------------- TERMINANDO A VERIFICACAO DOS ARQUIVOS ----------------------------");

    }

    public static void EscreverNoArq() throws IOException {
        try {
            File f = new File(String.valueOf(myPath));

            StringBuilder text = new StringBuilder();
            for (int i = 0; i < listDigestsCopyToWrite.size(); i++) {
                String[] info = listDigestsCopyToWrite.get(i);
                  for (int j = 0; j < info.length; j++)
                    text.append(info[j] + " ");
                text.append("\n");
            }
            FileWriter writer = new FileWriter(f, false);
            writer.write(text.toString());
            writer.close();
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }


    public static void main(String[] args) throws Exception {

//        if(args.length < 3) {
//            System.err.println("DigestCalculator Tipo_Digest Caminho_ArqListaDigest Caminho_da_Pasta_dos_Arquivos");
//            System.exit(1);
//        }


        TypeDigest("SHA-256");
        setPath("list_digest.txt");
        setFileList("pastaDigest");

        Verify();

    }
}
