//Rober
import java.security.PublicKey;
import java.util.ArrayList;

public class TxHandler {

    public static final int VALID=1;
    public static final int INVALID=-1;
    public static final int POT_VALID=0;

    public UTXOPool utxoPool;

    /**
     * Creates a public ledger whose current UTXOPool (collection of unspent transaction outputs) is
     * {@code utxoPool}. This should make a copy of utxoPool by using the UTXOPool(UTXOPool uPool)
     * constructor.
     */
    public TxHandler(UTXOPool utxoPool) {
        // IMPLEMENT THIS
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
        ArrayList<Transaction.Input> tx_in= tx.getInputs();
        double sum_in=0;

        ArrayList<UTXO> claim_utxo = new ArrayList<>();

        for (int i = 0; i < tx_in.size(); i++) {
            Transaction.Input in = tx_in.get(i);
            UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);

            //(1)     * (1) all outputs claimed by {@code tx} are in the current UTXO pool,
            if (!this.utxoPool.contains(utxo))
                return false;

            //(3)     * (3) no UTXO is claimed multiple times by {@code tx},
            if (claim_utxo.contains(utxo))
                return false;
            claim_utxo.add(utxo);


            Transaction.Output out=this.utxoPool.getTxOutput(utxo);

            PublicKey pk = out.address;
            byte[] msg = tx.getRawDataToSign(i);

            //(2)     * (2) the signatures on each input of {@code tx} are valid,
            if (!Crypto.verifySignature(pk,msg,in.signature))
                return false;
            sum_in+=out.value;
        }

        ArrayList<Transaction.Output> tx_out= tx.getOutputs();
        double sum_out=0;

        for (int i=0;i<tx_out.size();i++){

            double out_value= tx_out.get(i).value;
            //(4)     * (4) all of {@code tx}s output values are non-negative, and
            if (out_value<0) return false;
            sum_out+=out_value;
        }
        //(5)* (5) the sum of {@code tx}s input values is greater than or equal to the sum of its output values; 
        // //and false otherwise.
        if (sum_in < sum_out)
            return false;

        return true;
    }

    /**
     * Handles each epoch by receiving an unordered array of proposed transactions, checking each
     * transaction for correctness, returning a mutually valid array of accepted transactions, and
     * updating the current UTXO pool as appropriate.
     */
    public Transaction[] handleTxs(Transaction[] possibleTxs) {
        // IMPLEMENT THIS
        ArrayList<Transaction> v_Txs = new ArrayList<>();

        for (Transaction tx: possibleTxs)
            if(isValidTx(tx)){

                //remove claimed output
                for (Transaction.Input in: tx.getInputs()) {
                    UTXO utxo = new UTXO(in.prevTxHash, in.outputIndex);
                    this.utxoPool.removeUTXO(utxo);
                }

                //add in new output
                int out_index =0;
                for (Transaction.Output out: tx.getOutputs()) {
                    UTXO utxo = new UTXO(tx.getHash(), out_index++);
                    this.utxoPool.addUTXO(utxo, out);
                }

                v_Txs.add(tx);
            }

        int tx_size = v_Txs.size();
        Transaction [] valid_Txs = new Transaction[tx_size];
        for(int i = 0; i<tx_size; i++)
            valid_Txs[i] = v_Txs.get(i);

        return valid_Txs;
    }

}
