import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

public class Main {
    static int PC;
    static int R[] = new int[6];
    static int program[];

    public static void main(String[] args) {
        System.out.println("Hello RISC-V World!");
        readProgram("branch_less_than.txt");
        PC = 0;

        while(true) {
            if (PC >= program.length*4) {
                break;
            }

            int instr = program[PC/4];
            //Opcode
            int opcode = instr & 0x7f;
            int funct3 = (instr >> 12) & 0x07;
            int funct7 = instr >> 25;
            //Operands
            int rd = (instr >> 7) & 0x01f;
            int rs1 = (instr >> 15) & 0x01f;
            int rs2 = (instr >> 20) & 0x0f;
            //immidiates
            int imm = (instr >> 20);
            int imm_S = ((instr >> 20) & 0xfe0) | ((instr >> 7)& 0x1f);
            int imm_SB = 0xffffe000|((imm_S & 0x800)<<1) | ((imm_S & 0x001)<<11) | (imm_S & 0x7fe); // Clearify format

            switch (opcode) {
                case 0x13: //addi
                    R[rd] = R[rs1] + imm;
                    break;
                case 0x33:
                        switch (funct3) {
                            case 0x0: //add/sub
                                if (funct7 == 0) { //add
                                    R[rd] = R[rs1] + R[rs2];
                                    System.out.println("Adding: "+rs1+" + "+rs2+" to "+rd);
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
                            default:
                                System.out.println("Funct3 not found " + opcode + " " + funct3);
                                break;
                        }
                        break;
                case 0x23:
                    R[rd] = R[rs1] + imm;
                    break;
                default:
                    System.out.println("Opcode  " + opcode + " not yet implemented ");
                    break;
            }
            //Update program counter
            PC+=4;
            //Print register content
            for (int i = 0; i < R.length; ++i) {
                System.out.print(R[i] + " ");
            }
            System.out.println();
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
        program = new int[program_line];
        for (int j = 0; j < program_line; j++){
            program[j] = buffer[j];
            System.out.println("Instrution "+j+": "+program[j]);
        }
    }
}
