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
            readProgramBIN("tests/task1/addlarge.bin");
        } catch (IOException e) {
            e.printStackTrace();
        }
        PC = 0;
        boolean done = false;
        while(!done) {
            if (PC >= M.length*4) {
                break;
            }
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
        //Opcode
        int opcode = instr & 0x7f;
        int funct3 = (instr >> 12) & 0x07;
        int funct7 = (instr >> 25) & 0x7f;
        //Operands
        int rd = (instr >> 7) & 0x01f;
        int rs1 = (instr >> 15) & 0x01f;
        int rs2 = (instr >> 20) & 0x0f;
        //immidiates
        int imm = (instr >> 20);
        int imm_S = ((instr >> 20) & 0xfe0) | ((instr >> 7)& 0x1f);
        int imm_SB = 0xffffe000|((imm_S & 0x800)<<1) | ((imm_S & 0x001)<<11) | (imm_S & 0x7fe); // Clearify format
        int imm_U = (instr >> 12);
        System.out.printf("Opcode: %2x Func3: %1x\n",opcode,funct3);
        switch (opcode) {
            case 0x03:
                break;
            case 0x13: //addi
                System.out.println("Adding imm: "+imm+" to "+rd);
                R[rd] = R[rs1] + imm;
                break;
            case 0x17:
                break;
            case 0x1b:
                break;
            case 0x23:
                break;
            case 0x33:
                switch (funct3) {
                    case 0x0: //add/sub
                        if (funct7 == 0) { //add
                            R[rd] = R[rs1] + R[rs2];
                            System.out.println("Adding: "+rs1+" and "+rs2+" to "+rd);
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
                break;
            case 0x3b:
                break;
            case 0x63: //branch
                switch (funct3) {
                    case 0x0: //beq
                        if (R[rs1] == R[rs2]){
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x1: //bne
                        if (R[rs1] != R[rs2]){
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x4: //blt
                        if (R[rs1] < R[rs2]){
                            System.out.println("Branch: "+imm_SB);
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x5: //bge
                        if (R[rs1] >= R[rs2]){
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x6: //bltu
                        if (R[rs1] < R[rs2]){
                            System.out.println("Branch: "+imm_SB);
                            PC = PC+imm_SB-4;
                        }
                        break;
                    case 0x7: //bgeu
                        if (R[rs1] >= R[rs2]){
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
                break;
            default:
                done = true;
                System.out.println("Opcode  " + opcode + " not yet implemented ");
                break;
        }
        return done;
    }
}
