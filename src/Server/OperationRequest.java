package Server;

public class OperationRequest {
    private Operation operationType;//operation type of the request
    private int amount;//amount involved in the operation
    private String account;//account involved in the operation
    private String targetAccount;//target account for transfer operations (optional)
    
    public OperationRequest(Operation operationType, int amount, String account, String targetAccount) {
        this.operationType = operationType;
        this.amount = amount;
        this.account = account;
        this.targetAccount = targetAccount;
    }

    public Operation getOperationType() {
        return operationType;
    }

    public void setOperationType(Operation operationType) {
        this.operationType = operationType;
    }

    public int getAmount() {
        return amount;
    }

    public void setAmount(int amount) {
        this.amount = amount;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(String targetAccount) {
        this.targetAccount = targetAccount;
    }
}
