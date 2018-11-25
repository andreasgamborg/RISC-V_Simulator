import java.io.*;
import java.nio.file.*;
import java.util.Scanner;

public class Main {
    static int PC;
    static int R[] = new int[32];
    static int M[] = new int[1024];

    public static void main(String[] args) {
        System.out.println("Hello RISC-V World!");
        try {
            readProgramBIN("tests/task2/branchmany.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        PC = 0;
        boolean done = false;
        while(!done) {
            //Fetch
            int instr = M[PC/4];
            //Execute (break when finished)
            done = execute(instr);
            // Update program counter
            PC+=4;
            // Print register content
            printReg();
        }
        System.out.println("Program exit");
    }

    public static void readProgram(String program_file_path){
        int max_length = 100;
        Scanner scan = null;
        int buffer[] = new int[max_length];
        File program_file = new File(program_file_path);
        try {
            scan = new Scanner(program_file);
            scan.useRadix(16);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.out.println("Program file not found");
        }
        int program_line = 0;
        while (scan.hasNextLine()){
            if (program_line>=max_length){
                System.out.println("Program file too long");
                break;
            }
            buffer[program_line] = (int)scan.nextLong();
            program_line++;
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
        int rs1 = (instr >> 15) & 0x01f;
        int rs2 = (instr >> 20) & 0x0f;
        // Immidiates
        int imm_I = (instr >> 20);
        int imm_S = ((instr >> 20) & 0xfe0) | ((instr >> 7)& 0x1f);
        int imm_SB = (instr & 0x80000000) | ((instr << 23) & 0x40000000) |((instr >> 1) & 0x3f000000) | ((instr << 12) & 0x00f00000);
        imm_SB = imm_SB >> 19; // sign extension

        int imm_U = (instr >> 12);
        int imm_UJ = (instr >> 12);  // FIXME: 25-11-2018
        System.out.printf("Opcode: %2x Func3: %1x\n",opcode,funct3);
        // R0 must be 0
        R[0] = 0;
        // Compute instr
        switch (opcode) {
            case 0x00: //end of program
                System.out.println("End of program reached");
                done = true;
                break;
            case 0x03:
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
            //case 0x17:
                //break;
            //case 0x1b:
                //break;
            //case 0x23:
                //break;
            case 0x33:
                switch (funct3) {
                    case 0x0: //add/sub
                        if (funct7 == 0) { //add
                            R[rd] = R[rs1] + R[rs2];
                            System.out.println("Adding: R"+rs1+" and R"+rs2+" to R"+rd);
                        } else { //sub
                            R[rd] = R[rs1] - R[rs2];
                        }
                        break;
                    case 0x1: //sll
                        R[rd] = R[rs1]<<R[rs2];
                        break;
                    case 0x2: //slt
                        R[rd] = R[rs1] < R[rs2] ? 0x1 : 0x0;
                        break;
                    case 0x3: //sltu
                        R[rd] = R[rs1] < R[rs2] ? 0x1 : 0x0;
                        break;
                    case 0x4: //xor
                        R[rd] = R[rs1] ^ R[rs2];
                        break;
                    case 0x5: //srl
                        R[rd] = R[rs1]>>R[rs2];
                        break;
                    case 0x6: //or
                        R[rd] = R[rs1] | R[rs2];
                        break;
                    case 0x7: //and
                        R[rd] = R[rs1] & R[rs2];
                        break;
                    default:
                        System.out.println("Funct3 not found " + opcode + " " + funct3);
                        break;
                }
                break;
            case 0x37: //lui
                R[rd] &= 0xfffff<<12;
                R[rd] |= imm_U<<12;
                System.out.println("Add upper imm_I to R"+ rd);
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
                            System.out.println("Branch taken "+ imm_SB/4);
                            System.out.printf("%d : 0x%x\n",imm_SB,imm_SB);
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
                        if (R[rs1] < R[rs2]){
                            System.out.println("Branch taken");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x7: //bgeu
                        System.out.printf("bgeu R%d, R%d, %d \n",rs1,rs2,imm_SB);
                        if (R[rs1] >= R[rs2]){
                            System.out.println("Branch taken");
                            PC = PC+imm_SB-4;
                        }
                        break;
                    default:
                        System.out.println("Funct3 not found " + opcode + " " + funct3);
                        break;
                }
                break;
            //case 0x67:
                //break;
            //case 0x6f:
                //break;
            case 0x73: //ecall
                done = true;
                System.out.println("ecall");
                break;
            default:
                done = true;
                System.out.println("Opcode  " + opcode + " not yet implemented ");
                break;
        }
        return done;
    }
}
