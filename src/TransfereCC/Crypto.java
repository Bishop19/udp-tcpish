package TransfereCC;

import Common.Pair;

import javax.crypto.Cipher;
import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.*;
import java.security.spec.X509EncodedKeySpec;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;



public class Crypto {


    static KeyPair generateKeys() throws NoSuchProviderException, NoSuchAlgorithmException {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("DSA", "SUN");

        SecureRandom random = SecureRandom.getInstance("SHA1PRNG", "SUN");
        keyGen.initialize(1024, random);

        KeyPair pair = keyGen.generateKeyPair();

        return pair;
    }


    /******************* ASSINATURA DIGITAL *******************/

     static Pair<byte[], byte[]> generateSignature(String file, KeyPair pair) {

        try {
            PrivateKey priv = pair.getPrivate();
            PublicKey pub = pair.getPublic();

            Signature dsa = Signature.getInstance("SHA1withDSA", "SUN");

            dsa.initSign(priv);

            // Indicar o ficheiro para ser assinado
            FileInputStream fis = new FileInputStream(file);
            BufferedInputStream bufin = new BufferedInputStream(fis);
            byte[] buffer = new byte[1024];
            int len;
            while ((len = bufin.read(buffer)) >= 0) {
                dsa.update(buffer, 0, len);
            }
            bufin.close();

            // Gera assinatura
            byte[] real_sig = dsa.sign();
            byte[] key = pub.getEncoded();

            return new Pair<>(real_sig, key);
        }
        catch (Exception e) {
            System.err.println("Erro:" + e.toString());
        }


        return null;
    }


    static boolean verifySignature(String file, byte[] sig_to_verify, byte[] pubkey_bytes) {

        try {
            // Gera PublicKey a partir do byte[]
            PublicKey pub_key = KeyFactory.getInstance("DSA", "SUN").generatePublic(new X509EncodedKeySpec(pubkey_bytes));

            // Initialize the Signature Object for Verification
            Signature sig = Signature.getInstance("SHA1withDSA", "SUN");

            sig.initVerify(pub_key);

            // Supply the Signature Object With the Data to be Verified
            FileInputStream datafis = new FileInputStream(file);
            BufferedInputStream bufin = new BufferedInputStream(datafis);

            byte[] buffer = new byte[1024];
            int len;
            while (bufin.available() != 0) {
                len = bufin.read(buffer);
                sig.update(buffer, 0, len);
            }
            ;

            bufin.close();

            try {
                return sig.verify(sig_to_verify);
            } catch (SignatureException e) {

                e.printStackTrace();
                return false;
            }
        }
        catch (Exception e){
            return false;
        }



    }




    /******************* ENCRIPTAÇÃO DADOS *******************/

    public static byte[] encryptData(byte[] input, PublicKey pub_key) throws Exception{
        /*
        //Creating a Signature object
        Signature sign = Signature.getInstance("SHA256withRSA");

        //Creating KeyPair generator object
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

        //Initializing the key pair generator
        keyPairGen.initialize(1024);

        //Generating the pair of keys
        KeyPair pair = keyPairGen.generateKeyPair();
    */
        // Creating a Cipher object
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        // Initializing a Cipher object
        cipher.init(Cipher.ENCRYPT_MODE, pub_key);

        // Adding data to the cipher
        cipher.update(input);

        // encrypting the data
        return cipher.doFinal();
    }

    public static byte[] decryptData(byte[] input, PrivateKey priv_key) throws Exception{

        /*
        //Creating a Signature object
        Signature sign = Signature.getInstance("SHA256withRSA");

        //Creating KeyPair generator object
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

        //Initializing the key pair generator
        keyPairGen.initialize(2048);

        //Generate the pair of keys
        KeyPair pair = keyPairGen.generateKeyPair();

        //Getting the public key from the key pair
        PublicKey publicKey = pair.getPublic();


        //Initializing a Cipher object
        cipher.init(Cipher.ENCRYPT_MODE, publicKey);

        //Add data to the cipher
        byte[] input = "Welcome to Tutorialspoint".getBytes();
        cipher.update(input);

        //encrypting the data
        byte[] cipherText = cipher.doFinal();
        System.out.println( new String(cipherText, "UTF8"));
*/
        //Creating a Cipher object
        Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");

        //Initializing the same cipher for decryption
        cipher.init(Cipher.DECRYPT_MODE, priv_key);


        //Decrypting the text
        return cipher.doFinal(input);
    }

    /*
    public static List<byte[]> dividePacket(byte[] content, int max) throws IOException {

        int to_consume= content.length;
        int frag = content.length/max ;

        ArrayList<byte[]> fragmentos = new ArrayList<>();
        for(int i = 0; i<frag; i++) {
            fragmentos.add(Arrays.copyOfRange(content, i * max, i * max + max ));
            to_consume -= max;
        }
        //add last frag
        if(to_consume > 0) fragmentos.add(Arrays.copyOfRange(content,frag*max, content.length));

        return fragmentos;
    }

    public static void main(String args[]) throws Exception {
        LocalDateTime inicio = LocalDateTime.now();
        String input = "TESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsaTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsaTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsaTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsaTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsaTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsaTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsadfsfdskdfkdkdkdkdkdkdkkdkdkkddkkTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsadfsfdskdfkdkdkdkdkdkdkkdkdkkddkkdkdkkTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsadfsfdskdfkdkdkdkdkdkdkkdkdkkddkkdkdkkTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsadfsfdskdfkdkdkdkdkdkdkkdkdkkddkkdkdkkTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsadfsfdskdfkdkdkdkdkdkdkkdkdkkddkkdkdkkTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsadfsfdskdfkdkdkdkdkdkdkkdkdkkddkkdkdkkTESTEodfsTesteasdfjasdfjhasdhjasdfhoadsfadfsasdfadfsadfsdsafdfsaadfsdfsadfsfdskdfkdkdkdkdkdkdkkdkdkkddkkdkdkkdkdkkddkhoadfshohadsfhdsafakkk";
        byte[] input_bytes = input.getBytes();

        //Creating KeyPair generator object
        KeyPairGenerator keyPairGen = KeyPairGenerator.getInstance("RSA");

        List<byte[]> input_bytes_array = dividePacket(input_bytes, 117);
        //Initializing the key pair generator
        keyPairGen.initialize(1024);

        //Generate the pair of keys
        KeyPair pair = keyPairGen.generateKeyPair();

        //Getting the public key from the key pair
        PublicKey publicKey = pair.getPublic();

        int i = 0;
        for(byte[] b : input_bytes_array){
            System.out.println("NORMAL = " + new String(b) + " TAMANHO = " + b.length);

            b = encryptData(b, publicKey);

            System.out.println("ENCRIPTADO = " + new String(b) + " TAMANHO = " + b.length);

            b = decryptData(b, pair.getPrivate());

            System.out.println("DECRIPTADO = " + new String(b) + " TAMANHO = " + b.length);
            i++;
        }

        LocalDateTime fim = LocalDateTime.now();
        System.out.println("TOTAL = " + i);

        System.out.println(ChronoUnit.MILLIS.between(inicio, fim));

    }*/
}
