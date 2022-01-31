//part of ClassSutup project
//module STUDENTSPAGE


var table;
var selected = [];
var insert_mode=false;
var edit_mode=false;
var res = ["#name_to_replace", "#form_to_replace", "#forms","#surname","#name","#fname","#login", "#passw"];
var sending=false;



function initMe(jsonObject) {

    //trim polyfill
    if (!String.prototype.trim) {
        (function() {
            String.prototype.trim = function() {
                return this.replace(/^[\s\uFEFF\xA0]+|[\s\uFEFF\xA0]+$/g, '');
            };
        })();
    }

    // /tables launching
    table=$('#mainTable').DataTable( {
        "lengthMenu": [[8, 16, 32, -1], [8, 16, 32, "All"]],

        "aaData": jsonObject,

        "aoColumns": [
            { "mDataProp": "l_replace" },
            { "mDataProp": "f_replace" },
            { "mDataProp": "form" },
            { "mDataProp": "fname" },
            { "mDataProp": "sname" },
            { "mDataProp": "tname" },
            { "mDataProp": "login" },
            { "mDataProp": "paswd" }
        ],


	    "order": [[ 2, "asc" ], [3, "asc"]],

        "scrollY":        "21em",
        "scrollCollapse": true,
        "deferRender": true,

        "columnDefs": [
            {"targets": [ 0 ], "visible": false},
            {"targets": [ 1 ], "visible": false},
            {"targets": [ 7 ], "visible": false}
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
        //mark selection
        table.$('tr.selected').removeClass('selected');
        $(this).addClass('selected');

        //set values in inputs
        var pos = table.row( this ).index();
        for (var i=0; i<res.length; i++) {
            var psa = table.cells({row: pos, column: i}).data()[0];
            $(res[i]).val(psa);
        }
        //set header for modal dialog
        $("#myModalLabel").text("В какой класс переводим учеников класса "+$(res[2]).val()+"?");

        setSelect();
    });


    //set initial state of buttons
    $(".replacer, .deleter, .inserter, .totaldel, .to_changer").prop( "disabled", true );

    function setSelect () {
        $(".deleter, .totaldel, .to_changer").prop( "disabled", false );
        $(".inserter, .replacer").prop( "disabled", true );
        insert_mode=true;
        edit_mode=false;
    }




    //helpers
    //alert
    function alertMe(c) {
        switch (c) {
             case 1: bootbox.alert("Пустые или неверно заполненные поля! \nПравильный формат: 'Для ФИО разрешена только кирилица, для логина и пароля - латиница и цифры'");
    	     break;
	     case 2: bootbox.alert("Невозможно добавить ученика: логин уже занят");
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

    //validator
    function validator (if_new) {

        //check certain fields for sertain patterns
        function validator_input(i, s) {
            switch (true) {
                case (i<6 && i>2)  :   {
                    var patt = /^[А-Яа-яіІїЇєЄґҐ'`‘]+$/;
                    if (patt.test(s)) return true; else return false;
                }
                    break;
                case (i==6 || i==7)  :   {
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
            if (if_new && i<2) {valid++;}
            else if (typeof $(res[i]).val() == 'string')
                if ($(res[i]).val().length>0 && validator_input(i,$(res[i]).val())) valid++;
                else  $(res[i]).parent().addClass('has-error');
        }
        return valid==res.length;
    }



    //check for double login
    function double_checker(log_to_check, form_check) {
        var valid=true;
        var total_rows=table.column(0).data().length;
        for (var i=0; i<total_rows; i++)
            if (table.cells({row: i, column: 0}).data()[0]===log_to_check &&
                table.cells({row: i, column: 1}).data()[0]===form_check) valid = false;
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




    //unselected mode for buttons
    var cleanSelect = function  () {
        select=[];
        $(".deleter, .to_changer, .totaldel").prop( "disabled", true );
        $(".inserter").prop( "disabled", false );
        if (insert_mode) $(".replacer").prop( "disabled", false );
        edit_mode=true;
    };
    $("input").bind('input',cleanSelect);
    $("#forms").bind('change',cleanSelect);



    //page searching
    function setPage ( data1, data2, column1, column2, table ) {
        var pos=-1;
        var tablearray1 = table.column(column1, {order:'current'}).data();
        var tablearray2 = table.column(column2, {order:'current'}).data();
        for (var i=0; i<tablearray1.length; i++)
                if (tablearray1[i]==data1 && tablearray2[i]==data2) {pos=i; break;}

        if ( pos >= 0 ) {
            var page = Math.floor( pos / table.page.info().length );
            console.log(page);
            table.page( page ).draw( false );
        }

        return this;
    }





    //buttons handlers
    var inSerter = function () {
        trimmer();
        if (!validator(true)) {alertMe(1); return;}
        //check if we are inserting the same login to the same form
	    if (!double_checker($(res[6]).val(), $(res[2]).val())) {alertMe(2); return;}
        edit_mode=true;
        if (sending) return;
        sending=true;
        var form = document.getElementById("my_form");
        form.action=path2;
        form.submit();
    };
    $(".inserter").click(inSerter);


    var upDater = function () {
        trimmer();
        if (!validator(false)) {alertMe(1); return;}
        if (sending) return;
        sending=true;
        var form = document.getElementById("my_form");
        form.action=path3;
        form.submit();
    };
    $(".replacer").click(upDater);



    var deliTer = function () {
        if (edit_mode) {alertMe(1); return;}
        confirmMe("Вы хотите удалить ученика \n" + $(res[3]).val() + "?",
            function (result) {
                if (result) {
                    if (sending) return;
                    sending = true;
                    var form = document.getElementById("my_form");
                    form.action = path1;
                    form.submit();
                }
            }
        );
    };
    $(".deleter").click(deliTer);


    var totalDeliTer = function () {
        if (edit_mode) {alertMe(1); return;}
        confirmMe("Вы хотите удалить ВЕСЬ класс \n"+$(res[2]).val()+"?",
            function (result){
            if (result) {
                if (sending) return;
                sending=true;
                var form = document.getElementById("my_form");
                form.action=path4;
                form.submit();
            }
        });
    };
    $(".totaldel").click(totalDeliTer);


    var totalChanger = function () {
        if (edit_mode) {alertMe(1); return;}
        if (sending) return;
        sending=true;
        var form = document.getElementById("my_form");
        form.action=path5;
        form.submit();
    };
    $(".changer").click(totalChanger);




    //page to inserted (updated) item
    if (revertTo1!=undefined && revertTo2!=undefined)  {
        setPage( revertTo1, revertTo2, 6, 2, table);
        $('tr:has(td:contains("'+revertTo1+'"))').addClass('selected');
    }
}





