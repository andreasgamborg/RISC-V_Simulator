import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

public class Main {
    static int PC;
    static int R[] = new int[32];
    static int M[] = new int[524288*2];

    public static void main(String[] args) {
        Scanner scanin = new Scanner(System.in);
        boolean done = false;

        System.out.println("Enter path to program file");
        String program_path = scanin.nextLine();
        try {
            readProgramBIN(program_path);
        } catch (IOException e) {
            System.out.println("Could not open program file");
            done = true;
        }
        PC = 0;
        while(!done) {
            //Fetch
            int instr = M[PC/4];
            //Execute (break when finished)
            done = execute(instr);
            // Update program counter
            PC+=4;
            // Print register content
            printReg();
            if (done){
                System.out.println("Program exit");
                try {
                    writeOut("out.bin");
                } catch (IOException e) {
                    System.out.println("Could not write output file");
                }
            }
        }
    }

    private static void writeOut(String file_name) throws IOException {
        FileOutputStream file = new FileOutputStream(file_name);
        DataOutputStream out = new DataOutputStream(file);
        System.out.println("Writing output file...");
        for (int i = 0; i < R.length; ++i) {
            out.writeByte((R[i]&0x000000ff)>>0);
            out.writeByte((R[i]&0x0000ff00)>>8);
            out.writeByte((R[i]&0x00ff0000)>>16);
            out.writeByte((R[i]&0xff000000)>>24);
        }
        out.close();
    }

    public static void readProgram(String program_file_path){
        Scanner scan = null;
        File program_file = new File(program_file_path);
        try {
            scan = new Scanner(program_file);
            scan.useRadix(16);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Program file not found");
        }
        int i = 0;
        while (scan.hasNextLine()){
            M[i] = (int)scan.nextLong();
            i++;
        }
        scan.close();
    }
    public static void readProgramBIN(String program_file_path) throws IOException {
        Path path = Paths.get(program_file_path);
        byte[] in = Files.readAllBytes(path);
        System.out.println("Reading file:  "+path);

        for(int i = 0; i < in.length/4; i++){
            System.out.printf("%3d : ",i*4);
            M[i] =((int)(in[i*4+3]&0xff)<<24)|
                    ((int)(in[i*4+2]&0xff)<<16)|
                    ((int)(in[i*4+1]&0xff)<<8)|
                    ((int)in[i*4]&0xff);
            System.out.printf("%02x %02x %02x %02x ",in[i*4+3],in[i*4+2],in[i*4+1],in[i*4+0]);
            System.out.printf("  -> %5d\n",M[i]);
        }
    }
    public static void printReg(){
        for (int i = 0; i < R.length; ++i) {
            System.out.printf("%2d ",R[i]);
        }
        System.out.println();
        for (int i = 0; i < R.length; ++i) {
            System.out.print("------");
        }
        System.out.println();
    }
    public static boolean execute(int instr){
        boolean done = false;
        // Opcode
        int opcode = instr & 0x7f;
        int funct3 = (instr >> 12) & 0x07;
        int funct7 = (instr >> 25) & 0x7f;
        // Operands
        int rd = (instr >> 7) & 0x01f;
        int rs1 = (instr >> 15) & 0x1f;
        int rs2 = (instr >> 20) & 0x1f;
        // Immidiates
        int imm_I = instr & 0xfff00000;
        imm_I = imm_I >> 20;
        int imm_S = (instr & 0xfe000000) | ((instr << 13)& 0x01f00000);
        imm_S = imm_S >> 20;
        int imm_SB = (instr & 0x80000000) | ((instr << 23) & 0x40000000) |((instr >> 1) & 0x3f000000) | ((instr << 12) & 0x00f00000);
        imm_SB = imm_SB >> 19; // sign extension
        int imm_U = instr & 0xfffff000;
        int imm_UJ = (instr & 0x80000000) | ((instr << 11) & 0x7f800000) |((instr << 2) & 0x00400000) | ((instr >> 9) & 0x003ff000);
        imm_UJ = imm_UJ >> 11;
        System.out.printf("PC: %4d  | Opcode: 0x%02x  | Func3: 0x%1x\n",PC, opcode, funct3);
        // R0 must be 0
        R[0] = 0;
        // for unsigned
        long r1;
        long r2;
        // Compute instr
        switch (opcode) {
            case 0x00: //end of program
                System.out.println("End of program reached: no ecall");
                done = true;
                break;
            case 0x03:
                switch (funct3) {
                    case 0x0: //lb
                        System.out.printf("lb R%d, %d(R%d) \n",rd, imm_I, rs1);
                        R[rd] = M[(R[rs1]+imm_I)/4];
                        if((R[rs1]+imm_I)%4==0){
                            R[rd] &= 0x000000ff;
                        }
                        if((R[rs1]+imm_I)%4==1){
                            R[rd] &= 0x0000ff00;
                        }
                        if((R[rs1]+imm_I)%4==2){
                            R[rd] &= 0x00ff0000;
                        }
                        if((R[rs1]+imm_I)%4==3){
                            R[rd] &= 0xff000000;
                        }
                        break;
                    case 0x2: //lw
                        System.out.printf("lw R%d, %d(R%d) \n",rd, imm_I, rs1);
                        R[rd] = M[(R[rs1]+imm_I)/4];
                        break;
                    default:
                        System.out.println("Funct3 not found " + opcode + " " + funct3);
                        break;
                }
                break;
            case 0x13: //I-Format
                switch (funct3) {
                    case 0x0: //addi
                        System.out.printf("addi R%d, R%d, %d \n",rd,rs1,imm_I);
                        R[rd] = R[rs1] + imm_I;
                        break;
                    case 0x1: //slli
                        System.out.printf("slli R%d, R%d, %d \n",rd,rs1,imm_I);
                        R[rd] = R[rs1] << imm_I;
                        break;
                    case 0x2: //slti
                        System.out.printf("slti R%d, R%d, %d \n",rd,rs1,imm_I);
                        R[rd] = R[rs1] < imm_I ? 1 : 0;
                        break;
                    case 0x3: //sltiu
                        System.out.printf("sltiu R%d, R%d, %d \n",rd,rs1,imm_I);
                        r1 = R[rs1] & 0x00000000ffffffffL;
                        r2 = imm_I & 0x00000000ffffffffL;
                        System.out.printf("r1 %d r2 %d \n",r1,r2);
                        R[rd] = r1 < r2 ? 1 : 0;
                        break;
                    case 0x4: //xori
                        System.out.printf("xori R%d, R%d, %d \n",rd,rs1,imm_I);
                        R[rd] = R[rs1] ^ imm_I;
                        break;
                    case 0x5: //srli/srai
                        if (funct7 == 0) {
                            System.out.printf("srli R%d, R%d, %d \n",rd,rs1,imm_I);
                            R[rd] = R[rs1] >> imm_I;
                        }else{
                            System.out.printf("srai R%d, R%d, %d \n",rd,rs1,imm_I);
                            System.out.println("May be wrong!");
                            R[rd] = R[rs1] >> imm_I;
                        }

                        break;
                    case 0x6: //ori
                        System.out.printf("ori R%d, R%d, %d \n",rd,rs1,imm_I);
                        R[rd] = R[rs1] | imm_I;
                        break;
                    case 0x7: //andi
                        System.out.printf("andi R%d, R%d, %d \n",rd,rs1,imm_I);
                        R[rd] = R[rs1] & imm_I;
                        break;
                    default:
                        System.out.println("Funct3 not found " + opcode + " " + funct3);
                        break;
                }
                break;
            case 0x17:
                PC += imm_S;
                break;
            //case 0x1b:
                //break;
            case 0x23:
                switch (funct3) {
                    case 0x2: //sw
                        System.out.printf("sw R%d, %d(R%d) \n",rs2, imm_S, rs1);
                        M[(R[rs1]+imm_S)/4] = R[rs2];
                        break;
                    default:
                        System.out.println("Funct3 not found " + opcode + " " + funct3);
                        break;
                }
                break;
            case 0x33:
                switch (funct3) {
                    case 0x0: //add/sub
                        if (funct7 == 0) { //add
                            System.out.printf("add R%d, R%d, R%d \n",rd, rs1, rs2);
                            R[rd] = R[rs1] + R[rs2];
                        } else { //sub
                            System.out.printf("sub R%d, R%d, R%d \n",rd, rs1, rs2);
                            R[rd] = R[rs1] - R[rs2];
                        }
                        break;
                    case 0x1: //sll
                        System.out.printf("sll R%d, R%d, R%d \n",rd, rs1, rs2);
                        R[rd] = R[rs1]<<R[rs2];
                        break;
                    case 0x2: //slt
                        System.out.printf("slt R%d, R%d, R%d \n",rd, rs1, rs2);
                        R[rd] = R[rs1] < R[rs2] ? 0x1 : 0x0;
                        break;
                    case 0x3: //sltu
                        System.out.printf("sltu R%d, R%d, R%d \n",rd, rs1, rs2);
                        r1 = R[rs1] & 0x00000000ffffffffL;
                        r2 = R[rs2] & 0x00000000ffffffffL;
                        System.out.printf("r1 %d r2 %d \n",r1,r2);
                        R[rd] = r1 < r2 ? 0x1 : 0x0;
                        break;
                    case 0x4: //xor
                        System.out.printf("xor R%d, R%d, R%d \n",rd, rs1, rs2);
                        R[rd] = R[rs1] ^ R[rs2];
                        break;
                    case 0x5: //srl
                        System.out.printf("srl R%d, R%d, R%d \n",rd, rs1, rs2);
                        R[rd] = R[rs1]>>R[rs2];
                        break;
                    case 0x6: //or
                        System.out.printf("or R%d, R%d, R%d \n",rd, rs1, rs2);
                        R[rd] = R[rs1] | R[rs2];
                        break;
                    case 0x7: //and
                        System.out.printf("and R%d, R%d, R%d \n",rd, rs1, rs2);
                        R[rd] = R[rs1] & R[rs2];
                        break;
                    default:
                        System.out.println("Funct3 not found " + opcode + " " + funct3);
                        break;
                }
                break;
            case 0x37: //lui
                R[rd] &= 0x00000fff;
                R[rd] |= imm_U;
                System.out.printf("lui R%d, %d \n",rs1,imm_U);
                break;
            //case 0x3b:
                //break;
            case 0x63: //branch
                switch (funct3) {
                    case 0x0: //beq
                        System.out.printf("beq R%d, R%d, %d \n",rs1,rs2,imm_SB);
                        if (R[rs1] == R[rs2]){
                            System.out.println("Branch taken");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x1: //bne
                        System.out.printf("bne R%d, R%d, %d \n",rs1,rs2,imm_SB);
                        if (R[rs1] != R[rs2]){
                            System.out.println("Branch taken");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x4: //blt
                        System.out.printf("blt R%d, R%d, %d \n",rs1,rs2,imm_SB);
                        if (R[rs1] < R[rs2]){
                            System.out.println("Branch taken ");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x5: //bge
                        System.out.printf("bge R%d, R%d, %d \n",rs1,rs2,imm_SB);
                        if (R[rs1] >= R[rs2]){
                            System.out.println("Branch taken ");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x6: //bltu
                        System.out.printf("bltu R%d, R%d, %d \n",rs1,rs2,imm_SB);
                        r1 = R[rs1] & 0x00000000ffffffffL;
                        r2 = R[rs2] & 0x00000000ffffffffL;
                        if (r1 < r2){
                            System.out.println("Branch taken");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x7: //bgeu
                        System.out.printf("bgeu R%d, R%d, %d \n",rs1,rs2,imm_SB);
                        r1 = R[rs1] & 0x00000000ffffffffL;
                        r2 = R[rs2] & 0x00000000ffffffffL;
                        if (r1 >= r2){
                            System.out.println("Branch taken");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    default:
                        done = true;
                        System.out.printf("Funct3 - %2x not implemented for opcode %2x\n", funct3, opcode);
                        break;
                }
                break;
            case 0x67: //jalr
                System.out.printf("jalr R%d, R%d+%d \n",rd, rs1, imm_I);
                R[rd] = PC+4;
                PC = R[rs1] + imm_I - 4;
                break;
            case 0x6f: //jal
                System.out.printf("jal R%d, %d \n",rd,imm_UJ);
                R[rd] = PC+4;
                PC = PC + imm_UJ - 4;
                break;
            case 0x73: //ecall
                done = true;
                System.out.println("ecall");
                break;
            default:
                done = true;
                System.out.printf("Opcode : 0x%2x : not implemented\n",opcode);
                break;
        }
        return done;
    }
}
