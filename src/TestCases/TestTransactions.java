import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.Arrays;

public class TestTransactions {

    public static byte[] generateSignature(PrivateKey privKey, byte[] message) {
        try {
            Signature sig = Signature.getInstance("SHA256withRSA");
            sig.initSign(privKey);
            sig.update(message);
            return sig.sign();
        } catch (NoSuchAlgorithmException | InvalidKeyException | SignatureException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static void main(String[] args) throws NoSuchAlgorithmException, InvalidKeyException {
        PrivateKey privateKey;
        PublicKey publicKey;
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        KeyPair keyPair = keyGen.generateKeyPair();
        privateKey = keyPair.getPrivate();
        publicKey = keyPair.getPublic();

        UTXOPool utxoPool = new UTXOPool();
        Transaction transaction = new Transaction();
        transaction.addOutput(50, publicKey);
        transaction.addOutput(100, publicKey);
        transaction.setHash(new byte[]{1, 2, 3});

        Transaction transaction2 = new Transaction();
        transaction2.addOutput(100, publicKey);
        transaction2.addOutput(200, publicKey);
        transaction2.setHash(new byte[]{10, 20, 30});

        utxoPool.addUTXO(new UTXO(transaction.getHash(), 0), transaction.getOutput(0));
        utxoPool.addUTXO(new UTXO(transaction2.getHash(), 0), transaction2.getOutput(0));

        TxHandler txHandler = new TxHandler(utxoPool);

        Transaction newTransaction = new Transaction();
        newTransaction.setHash(new byte[]{8, 8, 8});
        newTransaction.addInput(transaction.getHash(), 0);
//        newTransaction.addInput(transaction.getHash(), 1);
        newTransaction.addOutput(50, publicKey);
        newTransaction.addSignature(generateSignature(privateKey, newTransaction.getRawDataToSign(0)), 0);
//        newTransaction.addSignature(generateSignature(privateKey, newTransaction.getRawDataToSign(1)), 1);

//        newTransaction.addInput(transaction.getHash(), 0);
//        newTransaction.addSignature(generateSignature(privateKey, newTransaction.getRawDataToSign(1)), 1);

        Transaction newTransaction2 = new Transaction();
        newTransaction2.setHash(new byte[]{5, 6, 7});
        newTransaction2.addInput(transaction2.getHash(), 0);
        newTransaction2.addOutput(50, publicKey);
        newTransaction2.addSignature(generateSignature(privateKey, newTransaction2.getRawDataToSign(0)), 0);

        Transaction newTransaction3 = new Transaction();
        newTransaction3.setHash(new byte[]{9, 9, 9});
        newTransaction3.addInput(newTransaction.getHash(), 0);
        newTransaction3.addOutput(50, publicKey);
        newTransaction3.addSignature(generateSignature(privateKey, newTransaction3.getRawDataToSign(0)), 0);


        System.out.println(txHandler.isValidTx(newTransaction3));
        System.out.println(txHandler.isValidTx(newTransaction));
        System.out.println(txHandler.isValidTx(newTransaction2));

        Transaction[] transactions = new Transaction[3];
        transactions[0] = newTransaction3;
        transactions[1] = newTransaction;
        transactions[2] = newTransaction2;

        Transaction[] validTransactions = txHandler.handleTxs(transactions);
        System.out.println(Arrays.toString(validTransactions));

        System.out.println(Arrays.equals(validTransactions, transactions));

    }
}