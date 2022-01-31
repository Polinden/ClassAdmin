//part of ClassSutup project
//module PREPODSPAGE


var table;
var selected = [];
var insert_mode=false;
var edit_mode=false;
var res = ["#name_to_replace", "#surname","#name","#fname","#login", "#passw"];
var sending=false;


//trim polifill
if (!String.prototype.trim) {
    (function() {
        String.prototype.trim = function() {
            return this.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '');
        };
    })();
}



$(document).ready(function() {


    //tables launching
    table=$('#mainTable').DataTable( {
        "lengthMenu": [[8, 16, 32, -1], [8, 16, 32, "All"]],

        "scrollY":        "21em",
        "scrollCollapse": true,
        deferRender:    true,

        "order": [[ 1, "asc" ]],

        "columnDefs": [
            {"targets": [ 5 ], "visible": false}
        ],

        initComplete: function () {
            this.api().columns().every( function () {
                var column = this;
                var select = $('<select><option value=""></option></select>')
                    .appendTo( $(column.footer()).empty() )
                    .on( 'change', function () {
                        var val = $.fn.dataTable.util.escapeRegex(
                            $(this).val()
                        );
                        column
                            .search( val ? '^'+val+'$' : '', true, false )
                            .draw();
                    } );
                column.data().unique().sort().each( function ( d, j ) {
                    select.append( '<option value="'+d+'">'+d+'</option>' )
                } );
            } );
        },
        language: {
            "sProcessing": "Зачекайте...",
            "sLengthMenu": "Показати _MENU_ записів",
            "sZeroRecords": "Записи відсутні.",
            "sInfo": "Записи з _START_ по _END_ із _TOTAL_ записів",
            "sInfoEmpty": "Записи з 0 по 0 із 0 записів",
            "sInfoFiltered": "(відфільтровано з _MAX_ записів)",
            "sInfoPostFix": "",
            "sSearch": "Пошук:",
            "sUrl": "",
            "oPaginate": {
                "sFirst": "Перша",
                "sPrevious": "Попередня",
                "sNext": "Наступна",
                "sLast": "Остання"
            },
            "oAria": {
                "sSortAscending": ": активувати для сортування стовпців за зростанням",
                "sSortDescending": ": активувати для сортування стовпців за спаданням"
            }
        }
    });

    //fade in filled table
    $('.for_spinner').fadeTo(500, 1, function(){$(".spinner").css('display','none');});


    //selecting row handler
    $("#mainTable tbody").on( 'click', 'tr', function (){
        //select
        table.$('tr.selected').removeClass('selected');
        $(this).addClass('selected');

        //fill the form
        var td = $(this).find("td");
        var tl = td.length;
        for (var i=0; i<tl; i++) {
            var s = td[i].firstChild.data;
            $(res[i]).val(s.trim());
            selected[i]=s;
        }

        //fill hidden data
        var pos = table.row( this ).index();
        var psa = table.cells({ row: pos, column: 5 }).data()[0];
        $(res[5]).val(psa);


        setSelect();
    });


    //set initial state of buttons
    $(".replacer, .scheduler, .deleter, .inserter").prop( "disabled", true );



    //helpers
    //alert
    function alertMe(c) {
        switch (c) {
             case 1:  bootbox.alert("Пустые или неверно заполненные поля! \nПравильный формат: 'Для ФИО разрешена только кирилица, для логина и пароля - латиница и цифры'");
             break;
             case 2: bootbox.alert("Невозможно добавить учителя: логин уже занят");
             break;
        }
    }

    //confirm
    function confirmMe(text, callback){
        bootbox.confirm({
            message: text,
            buttons: {
                cancel: {
                    label: 'Нет',
                    className: 'btn-success'
                },
                confirm: {
                    label: 'Да',
                    className: 'btn-danger'
                }
            },
            callback: callback
        });
    }

    //page searching
    function setPage ( data, column, table ) {
        var pos = table.column(column, {order:'current'}).data().indexOf( data );

        if ( pos >= 0 ) {
            var page = Math.floor( pos / table.page.info().length );
            console.log(page);
            table.page( page ).draw( false );
        }

        return this;
    }


    //validators
    function validator (if_new) {

        //check certain fields for sertain patterns
        function validator_input(i, s) {
            switch (true) {
                case (i<4 && i>0)  :   {
                    var patt = /^[А-Яа-яіІїЇєЄґҐ'`‘]+$/;
                    if (patt.test(s)) return true; else return false;
                }
                    break;
                case (i==5 || i==4)  :   {
                    var patt = /^[A-Za-z0-9]+$/;
                    if (patt.test(s)) return true; else return false;
                }
                    break;
                default :   return true;
            }
        }

        //general check for emptiness and certain check by colling subfunction
        var valid=0;
        for (var i=0; i<res.length; i++) {
            $(res[i]).parent().removeClass('has-error');
            if (if_new && i==0) {valid++;}
            else if (typeof $(res[i]).val() == 'string')
                if ($(res[i]).val().length>0 && validator_input(i,$(res[i]).val())) valid++;
                else  $(res[i]).parent().addClass('has-error');
        }
        return valid==res.length;
    }

    //check for double login
    function double_checker(log_to_check) {
        var valid=true;
        var total_rows=table.column(0).data().length;
        for (var i=0; i<total_rows; i++) if (table.cells({row: i, column: 0}).data()[0]===log_to_check) valid = false;
        return valid;
    }


    //trimmer - delete spaces from beginnings and ends
    function trimmer () {
        for (var i=1; i<res.length; i++){
            if (typeof $(res[i]).val() == 'string') {
                var s = $(res[i]).val();
                $(res[i]).val(s.trim());
            }
        }
    }



    //selected mode for buttons
    function setSelect () {
        $(".deleter, .scheduler").prop( "disabled", false );
        $(".inserter, .replacer").prop( "disabled", true );
        insert_mode=true;
        edit_mode=false;
    }


    //unselected mode for buttons
    var cleanSelect = function  () {
        select=[];
        $(".deleter, .scheduler").prop( "disabled", true );
        $(".inserter").prop( "disabled", false );
        if (insert_mode) $(".replacer").prop( "disabled", false );
        edit_mode=true;
    };
    $("input").bind('input',cleanSelect);







    //handlers dor buttons
    var inSerter = function () {
        trimmer();
        if (!validator(true)) {alertMe(1); return;}
	    //check if we are insetring the same login as is present
        if (!double_checker($(res[4]).val())) {alertMe(2); return;}
        edit_mode=true;
        if (sending) return;
        sending=true;
        var form = document.getElementById("my_form");
        form.action=path3;
        form.submit();
    };
    $(".inserter").click(inSerter);

    var upDater = function () {
        trimmer();
        if (!validator(false)) {alertMe(1); return;}
        if (sending) return;
        sending=true;
        var form = document.getElementById("my_form");
        form.action=path4;
        form.submit();
    };
    $(".replacer").click(upDater);


    var deliTer = function () {
        if (edit_mode) {alertMe(1); return;}
        confirmMe("Вы хотите удалить преподавателя \n"+$(res[1]).val()+"?", function(result){
                if (result) {
                    if (sending) return;
                    sending = true;
                    var form = document.getElementById("my_form");
                    form.action = path2;
                    form.submit();
                }
        });
    };
    $(".deleter").click(deliTer);


    var scheDuler = function () {
        if (edit_mode) {alertMe(1); return;}
        if ($('#selected_subject').val().length==0) {alertMe(); return;}
        if (sending) return;
        sending=true;
        var form = document.getElementById("my_form");
        form.action=path1;
        form.submit();
    };
    $(".scheduler2").click(scheDuler);






    //page to inserted (updated) item
    if (revertTo!=undefined) {
        setPage( revertTo, 4, table);
        $('tr:has(td:contains("'+revertTo+'"))').addClass('selected');
    }
});






