/* ResultFactory.java
 *
 * A factory class to generate Result objects.
 * Injects a representation of the CPU status in every Result object that's created. 
 * 
 * (c) 2020 Andrea Spadaccini
 *
 * This file is part of the EduMIPS64 project, and is released under the GNU
 * General Public License.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package org.edumips64.client;

import java.util.Map;
import java.util.logging.Logger;

import com.google.gwt.json.client.JSONArray;

import org.edumips64.core.CPU;
import org.edumips64.core.Memory;
import org.edumips64.core.Register;
import org.edumips64.core.CPU.CPUStatus;
import org.edumips64.core.Pipeline.Stage;
import org.edumips64.core.fpu.RegisterFP;
import org.edumips64.core.is.InstructionInterface;
import org.edumips64.core.parser.ParserMultiException;
import org.edumips64.utils.io.StringWriter;

import elemental2.core.JsArray;
import jsinterop.base.Js;

public class ResultFactory {
    private CPU cpu;
    private Memory memory;
    private Logger logger = Logger.getLogger("ResultFactory");
    private StringWriter stdout;
    
    static String FromCpuStatus(CPUStatus s) {
        switch (s) {
        case READY:
            return "READY";
        case RUNNING:
        case STOPPING:
            return "RUNNING";
        default:
            return "STOPPED";
        }
    }

    public ResultFactory(CPU cpu, Memory memory, StringWriter stdout) {
        this.cpu = cpu;
        this.memory = memory;
        this.stdout = stdout;
    }

    public Result Success() {
        Result r = new Result(true, "", stdout.toString());
        return AddParsedInstructions(AddCpuInfo(r));
    }

    public Result Failure(String errorMessage) {
        Result r = new Result(false, errorMessage, stdout.toString());
        return AddParsedInstructions(AddCpuInfo(r));
    }

    public static Result AddParserErrors(Result result, ParserMultiException e) {
        if (e == null) return result;
        result.parsingErrors = Js.cast(e.getExceptionList().stream()
            .map(exception -> ParserErrorFactory.FromParserException(exception)).toArray(ParserError[]::new));
        return result;
    }

    private Result AddParsedInstructions(Result r) {
        r.parsedInstructions = new JsArray<>();
        int count = this.memory.getInstructionsNumber();
        for (int i = 0; i < count; ++i) {
            r.parsedInstructions.setAt(i, Instruction.FromInstruction(this.memory.getInstruction(i*4)));
        }
        return r;
    }

    private Result AddCpuInfo(Result r) {
        r.status = FromCpuStatus(cpu.getStatus());
        r.pipeline = getPipeline();
        r.memory = getMemory();
        r.registers = getRegisters();
        r.statistics = getStatistics();
        return r;
    }

    private String getMemory() {
        return memory.toString();
    }

    private String getRegisters() {
        var registers = new FluentJsonObject();

        try {
            // General Purpose Registers (GPR).
            int i = 0;
            JSONArray jsonGeneralRegisters = new JSONArray();
            for(Register r : cpu.getRegisters()) {
                jsonGeneralRegisters.set(i++,
                new FluentJsonObject()
                    .put("name", r.getName())
                    .put("alias", r.getAlias())
                    .put("hexString", r.getHexString())
                    .put("value", r.getValue())
                    .toJsonObject());
            }
            registers.put("gpr", jsonGeneralRegisters);

            // FPU registers.
            i = 0;
            JSONArray jsonFpuRegisters = new JSONArray();
            for(RegisterFP r : cpu.getRegistersFP()) {
                jsonFpuRegisters.set(i++,
                new FluentJsonObject()
                    .put("name", r.getName())
                    .put("hexString", r.getHexString())
                    .put("value", r.getValue())
                    .toJsonObject());
            }
            registers.put("fpu", jsonFpuRegisters);

            // Special registers (hi/lo/fcsr).
            i = 0;
            JSONArray specialRegisters = new JSONArray();
            specialRegisters.set(i++,
                new FluentJsonObject()
                    .put("name", cpu.getLO().getName())
                    .put("hexString", cpu.getLO().getHexString())
                    .put("value", cpu.getLO().getValue())
                    .toJsonObject());
            specialRegisters.set(i++,
                new FluentJsonObject()
                    .put("name", cpu.getHI().getName())
                    .put("hexString", cpu.getHI().getHexString())
                    .put("value", cpu.getHI().getValue())
                    .toJsonObject());
            specialRegisters.set(i++,
                new FluentJsonObject()
                    .put("name", "FCSR")
                    .put("value", cpu.getFCSR().getHexString())
                    .toJsonObject());
            registers.put("special", specialRegisters);
        } catch (Exception e) {
            logger.warning("Error fetching registers: " + e.toString());
        }
        return registers.toString();
    }

    private String getStatistics() {
        return new FluentJsonObject()
            // Execution
            .put("cycles", cpu.getCycles())
            .put("instructions", cpu.getInstructions())
            // Stalls
            .put("rawStalls", cpu.getRAWStalls())
            .put("wawStalls", cpu.getWAWStalls())
            .put("dividerStalls", cpu.getStructuralStallsDivider())
            .put("memoryStalls", cpu.getStructuralStallsMemory())
            // Code size
            .put("codeSizeBytes",memory.getInstructionsNumber() * 4)
            // FPU Control Status Register (FCSR)
            .put("fcsr", cpu.getFCSR().getBinString())
            .toString();
    }

    private Pipeline getPipeline() {
        // Convert the internal CPU representation to objects available to the JS code.
        Map<Stage, InstructionInterface> cpuPipeline = cpu.getPipeline();

        Pipeline p = new Pipeline();
        p.IF = Instruction.FromInstruction(cpuPipeline.get(Stage.IF));
        p.ID = Instruction.FromInstruction(cpuPipeline.get(Stage.ID));
        p.EX = Instruction.FromInstruction(cpuPipeline.get(Stage.EX));
        p.MEM = Instruction.FromInstruction(cpuPipeline.get(Stage.MEM));
        p.WB = Instruction.FromInstruction(cpuPipeline.get(Stage.WB));
        
        p.FPAdder1 = Instruction.FromInstruction(cpu.getFpuInstruction("ADDER", 1));
        p.FPAdder2 = Instruction.FromInstruction(cpu.getFpuInstruction("ADDER", 2));
        p.FPAdder3 = Instruction.FromInstruction(cpu.getFpuInstruction("ADDER", 3));
        p.FPAdder4 = Instruction.FromInstruction(cpu.getFpuInstruction("ADDER", 4));
        p.FPMultiplier1 = Instruction.FromInstruction(cpu.getFpuInstruction("MULTIPLIER", 1));
        p.FPMultiplier2 = Instruction.FromInstruction(cpu.getFpuInstruction("MULTIPLIER", 2));
        p.FPMultiplier3 = Instruction.FromInstruction(cpu.getFpuInstruction("MULTIPLIER", 3));
        p.FPMultiplier4 = Instruction.FromInstruction(cpu.getFpuInstruction("MULTIPLIER", 4));
        p.FPMultiplier5 = Instruction.FromInstruction(cpu.getFpuInstruction("MULTIPLIER", 5));
        p.FPMultiplier6 = Instruction.FromInstruction(cpu.getFpuInstruction("MULTIPLIER", 6));
        p.FPMultiplier7 = Instruction.FromInstruction(cpu.getFpuInstruction("MULTIPLIER", 7));
        p.FPDivider = Instruction.FromInstruction(cpu.getFpuInstruction("DIVIDER", 0));

        return p;
    }
}