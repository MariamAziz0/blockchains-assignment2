import java.security.PublicKey;
import java.util.HashMap;

/**
 * I acknowledge that I am aware of the academic integrity guidelines of this
 *  course, and that I worked on this assignment independently without any
 *  unauthorized help with coding or testing. - Mariam Aziz Gerges Zaki Sorial
 */

public class TxHandler {

    private UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. 
     */
    public TxHandler(UTXOPool utxoPool) {
        this.utxoPool = new UTXOPool(utxoPool);
    }

    /**
     * @return true if:
     * (1) all outputs claimed by {@code tx} are in the current UTXO pool, 
     * (2) the signatures on each input of {@code tx} are valid, 
     * (3) no UTXO is claimed multiple times by {@code tx},
     * (4) all of {@code tx}s output values are non-negative, and
     * (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output
     *     values; and false otherwise.
     */
    public boolean isValidTx(Transaction tx) {
        // IMPLEMENT THIS
        double input_values = 0, output_values = 0;
        HashMap<UTXO, Boolean> takenUTXOs = new HashMap<>();
        UTXO currentInputUTXO;

        // Check all outputs claimed by {@code tx} are in the current UTXO pool, and no UTXO is claimed multiple times by {@code tx}
        for (Transaction.Input input : tx.getInputs()) {
            currentInputUTXO = new UTXO(input.prevTxHash, input.outputIndex);

            if (takenUTXOs.containsKey(currentInputUTXO) || !utxoPool.contains(currentInputUTXO)) {
                return false;
            }

            takenUTXOs.put(currentInputUTXO, true);
            input_values += utxoPool.getTxOutput(currentInputUTXO).value;
        }

        // Check the signatures on each input of {@code tx} are valid
        for (int i = 0; i < tx.numInputs(); i++) {
            Transaction.Input input = tx.getInput(i);
            currentInputUTXO = new UTXO(input.prevTxHash, input.outputIndex);
            PublicKey publicKey = utxoPool.getTxOutput(currentInputUTXO).address;
            byte[] message = tx.getRawDataToSign(i);

            if (!Crypto.verifySignature(publicKey, message, input.signature)) {
                return false;
            }
        }

        // Check all of {@code tx}s output values are non-negative
        for (Transaction.Output output : tx.getOutputs()) {
            if (output.value < 0) {
                return false;
            }

            output_values += output.value;
        }

        // Check the sum of {@code tx}s input values is greater than or equal to the sum of its output values
        if (input_values < output_values) {
            return false;
        }

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS

        return null;
    }

    public UTXOPool getUTXOPool() {
        return utxoPool;
    }

}
