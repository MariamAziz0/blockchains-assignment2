/**
 * I acknowledge that I am aware of the academic integrity guidelines of this
 *  course, and that I worked on this assignment independently without any
 *  unauthorized help with coding or testing. - Mariam Aziz Gerges Zaki Sorial
 */

/**
 * Answer to the exercise in the assignment "The way the hash of the COINBASE transactions is computed in the provided code
 *  could lead to an issue. Identify the problematic scenario, and search for how the actual
 *  implementation of Bitcoin prevents it":

 *  Ans: The hash here is computed from one output which includes the value of the coin and the public key only.
 *  So, if the same public key has multiple coinbase transactions of the same value of the coin,
 *  they will have the same hash. So, the hash of the transaction isn't unique here.
 *  Bitcoin prevents this by adding one input to the transaction. This input includes prevTxHash as zeros,
 *  outputIndex set to the maximum value, and the signature must include the height of the current block it's included in.
 *  Reference: https://learnmeabitcoin.com/technical/mining/coinbase-transaction/
 */

// The BlockChain class should maintain only limited block nodes to satisfy the functionality.
// You should not have all the blocks added to the block chain in memory 
// as it would cause a memory overflow.

import java.util.*;

public class BlockChain {
    public static final int CUT_OFF_AGE = 10;
    private TransactionPool transactionPool;
    private ArrayList<BlockWrapper> blockChainHead;
    private BlockWrapper maxHeightBlockWrapper;

    /**
     * create an empty blockchain with just a genesis block. Assume {@code genesisBlock} is a valid
     * block
     */
    public BlockChain(Block genesisBlock) {
        // IMPLEMENT THIS
        transactionPool = new TransactionPool();
        blockChainHead = new ArrayList<>();
        maxHeightBlockWrapper = new BlockWrapper(genesisBlock, 1, getUTXOPoolForBlock(genesisBlock));
        blockChainHead.add(maxHeightBlockWrapper);
    }

    /** Get the maximum height block */
    public Block getMaxHeightBlock() {
        // IMPLEMENT THIS
        return maxHeightBlockWrapper.getBlock();
    }

    /** Get the UTXOPool for mining a new block on top of max height block */
    public UTXOPool getMaxHeightUTXOPool() {
        // IMPLEMENT THIS
        return maxHeightBlockWrapper.getUtxoPool();
    }

    /** Get the transaction pool to mine a new block */
    public TransactionPool getTransactionPool() {
        // IMPLEMENT THIS
        return transactionPool;
    }

    /**
     * Add {@code block} to the blockchain if it is valid. For validity, all transactions should be
     * valid and block should be at {@code height > (maxHeight - CUT_OFF_AGE)}, where maxHeight is 
     * the current height of the blockchain.
	   * <p>
	   * Assume the Genesis block is at height 1.
       * For example, you can try creating a new block over the genesis block (i.e. create a block at
	   * height 2) if the current blockchain height is less than or equal to CUT_OFF_AGE + 1. As soon as
	   * the current blockchain height exceeds CUT_OFF_AGE + 1, you cannot create a new block at height 2.
     * 
     * @return true if block is successfully added
     */
    public boolean addBlock(Block block) {
        // IMPLEMENT THIS
        if (block.getPrevBlockHash() == null) {
            return false;
        }

        if (!isValidCoinBaseTransaction(block.getCoinbase())) {
            return false;
        }

        BlockWrapper parentBlockWrapper = getParentBlockWrapper(block.getPrevBlockHash());

        if (parentBlockWrapper == null) {
            return false;
        }

        int currentBlockHeight = parentBlockWrapper.getHeight() + 1;
        if (currentBlockHeight <= maxHeightBlockWrapper.getHeight() - CUT_OFF_AGE) {
            return false;
        }

        TxHandler txHandler = new TxHandler(parentBlockWrapper.getUtxoPool());
        if (!isValidBlockTransactions(block, txHandler)) {
            return false;
        }

        handleAddingNewBlock(block, parentBlockWrapper, txHandler.getUTXOPool());
        return true;
    }

    /** Add a transaction to the transaction pool */
    public void addTransaction(Transaction tx) {
        // IMPLEMENT THIS
        transactionPool.addTransaction(tx);
    }

    private BlockWrapper getParentBlockWrapper(byte[] parentHash) {
        Queue<BlockWrapper> blockWrapperQueue = new LinkedList<>(blockChainHead);
        while (!blockWrapperQueue.isEmpty()) {
            BlockWrapper blockWrapper = blockWrapperQueue.poll();
            if (Arrays.equals(blockWrapper.getBlock().getHash(), parentHash)) {
                return blockWrapper;
            }

            blockWrapperQueue.addAll(blockWrapper.getChildren());
        }

        return null;
    }

    private boolean isValidCoinBaseTransaction(Transaction tx) {
      return tx.numOutputs() == 1 && tx.getOutput(0).value == Block.COINBASE;
    }

    /**
     * The block has only one coinBase transaction
     * If the transaction is not coinbase, it must have the UTXO in utxoPool
     * Check for double spending case
     */
    private boolean isValidBlockTransactions(Block block, TxHandler txHandler) {
        Transaction[] blockTransactions = block.getTransactions().toArray(new Transaction[0]);
        Transaction[] handledTransactions = txHandler.handleTxs(blockTransactions);

        return Arrays.equals(blockTransactions, handledTransactions);
    }

    // May we need to the maxHeightBlock or the blockchainHead, it will depend on the height
    private void handleAddingNewBlock(Block block, BlockWrapper parentBlockWrapper, UTXOPool utxoPool) {
        int currentBlockHeight = parentBlockWrapper.getHeight() + 1;

        removeTransactionsFromTransactionPool(block);

        utxoPool.addUTXO(new UTXO(block.getCoinbase().getHash(), 0), block.getCoinbase().getOutput(0));

        BlockWrapper currentBlockWrapper = new BlockWrapper(block, currentBlockHeight, utxoPool);
        parentBlockWrapper.addChild(currentBlockWrapper);

        updateHeadOrMaxHeightBlock(currentBlockWrapper);
    }

    private void updateHeadOrMaxHeightBlock(BlockWrapper blockWrapper) {
        if (blockWrapper.getHeight() > maxHeightBlockWrapper.getHeight()) {
            maxHeightBlockWrapper = blockWrapper;
        }

        int currentHeadHeight = blockChainHead.get(0).getHeight();
        if (maxHeightBlockWrapper.getHeight() - currentHeadHeight >= CUT_OFF_AGE + 1) {

            ArrayList<BlockWrapper> newBlockchainHead = new ArrayList<>();
            for (BlockWrapper newBlock : blockChainHead) {
                newBlockchainHead.addAll(newBlock.getChildren());
            }

            blockChainHead = newBlockchainHead;
        }
    }

    private UTXOPool getUTXOPoolForBlock(Block block) {
        UTXOPool utxoPool = new UTXOPool();
        utxoPool.addUTXO(new UTXO(block.getCoinbase().getHash(), 0), block.getCoinbase().getOutput(0));

        for (Transaction tx : block.getTransactions()) {
            for (int i = 0; i < tx.numOutputs(); i++) {
                utxoPool.addUTXO(new UTXO(tx.getHash(), i), tx.getOutput(i));
            }
        }
        return utxoPool;
    }

    private void removeTransactionsFromTransactionPool(Block block) {
        for (Transaction tx : block.getTransactions()) {
            transactionPool.removeTransaction(tx.getHash());
        }
    }

    // New class BlockWrapper to save more about each block
    private class BlockWrapper {
        private Block block;
        private int height;
        private ArrayList<BlockWrapper> children;
        private UTXOPool utxoPool;

        public BlockWrapper(Block block, int height, UTXOPool utxoPool) {
            this.block = block;
            this.height = height;
            this.children = new ArrayList<>();
            this.utxoPool = utxoPool;
        }

        public Block getBlock() {
            return block;
        }

        public int getHeight() {
            return height;
        }

        public ArrayList<BlockWrapper> getChildren() {
            return children;
        }

        public void addChild(BlockWrapper child) {
            children.add(child);
        }

        public UTXOPool getUtxoPool() {
            return utxoPool;
        }
    }
}