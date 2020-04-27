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

public class Checker {

    private ArrayList<String> pathFiles;
    private ArrayList<String[]> listaDigests; //lista de String[] para que possa ler o arquivo em partes
    private MessageDigest myDigest;
    private Path myPath;

    private enum Status {
        UNKNOW,
        OK,
        NOT_OK,
        NOT_FOUND,
        COLLISION,
    }

    public void TypeDigest(String type) throws NoSuchAlgorithmException { // processa Tipo_Digest podendo ser MD5 ou SHA1
        myDigest = MessageDigest.getInstance(type);
        System.out.println(myDigest.getProvider());
        System.out.println("-------------------------------------------------------");
    }

    public void setPath(String pathArqListaDigest) throws IOException { // processa Caminho_ArqListaDigest
        myPath = Paths.get(pathArqListaDigest);
        listaDigests = new ArrayList<String[]>();

        List<String> lines = Files.readAllLines(myPath);
        for(String line: lines) {
            listaDigests.add(line.split(""));
        }

        System.out.println("\nCada linha do arquivo: ");
        listaDigests.forEach(lineInformation -> {
            System.out.println(Arrays.toString(lineInformation));
        });
        System.out.println("-------------------------------------------------------");
    }

    public void setFileList (ArrayList<String> filesPaths) { // processa Caminho_da_Pasta_dos_Arquivos
        pathFiles = filesPaths;
    }

    private String DigestValueFromFile (Path filePath) throws IOException {
        if(Files.exists(filePath) == false) {
            System.err.print("FILE DOESN'T EXIST, EXITING \n");
            System.exit(2);
        }

        String digestHexa = "";

        byte[] fileBytes = Files.readAllBytes(filePath);
        myDigest.update(fileBytes, 0, fileBytes.length);
        fileBytes = myDigest.digest();

        for(int i = 0; i < fileBytes.length; i++)
            digestHexa = digestHexa + String.format("%02X", fileBytes[i]);

        return digestHexa;
    }

    private String DigestFromDigestsFile(int index) throws Exception {
        String[] information = listaDigests.get(index);
        for(int i = 1; i < information.length; i=i+2) {
            String type = information[i];

            if(type.equalsIgnoreCase(myDigest.getAlgorithm()))
                return information[i+1];
        }

        return null;
    }

    private Status FileStatus(String fileName, String fileDigest) throws Exception {
        Status status = Status.UNKNOW;

        // Confere digest com os arquivos passados
        for(int i = 0; i < pathFiles.size(); i++) {
            Path pathArq = Paths.get(pathFiles.get(i));
            String arqName = pathArq.getFileName().toString();

            if(fileName.equals(arqName)) // ignora o proprio arquivo
                continue;

            if(fileDigest.equalsIgnoreCase(DigestValueFromFile(pathArq))) // perguntar ao prof se faz diferença
            {
                status = Status.COLLISION;
            }
        }

        // Confere digest com lista de digests
        for(int i = 0; i < listaDigests.size(); i++) {
            String nameFileFromDigestsFile = listaDigests.get(i)[0];
            String digestFromFileOfDigestsFile = DigestFromDigestsFile(i);

            if(fileName.equals(nameFileFromDigestsFile) && status == Status.UNKNOW) {
                if(fileDigest.equals(digestFromFileOfDigestsFile)){
                    status = Status.OK;
                }
                else if (digestFromFileOfDigestsFile != null) {
                    status = Status.NOT_OK;
                }
            }
            else if (fileDigest.equalsIgnoreCase(digestFromFileOfDigestsFile)) {
                status = Status.COLLISION;
            }
        }

        if(status == Status.UNKNOW)
            status = Status.NOT_FOUND;

        return status;
    }

    public void ExecuteCheck () throws Exception {
        boolean notFound = false;
        for( int i = 0; i < pathFiles.size(); i++) {
            Path file = Paths.get(pathFiles.get(i));

            String fileName = file.getFileName().toString();
            String fileDigest = DigestValueFromFile(file);
            Status status = FileStatus(fileName, fileDigest);

            if (status == Status.NOT_FOUND) {
                notFound = true;
                // adicionar o arquivo no arquivo de digests
            }
        }

        if(notFound) {
            //reescrever o arquivo de digests para atualizar o mesmo
        }
    }
}
